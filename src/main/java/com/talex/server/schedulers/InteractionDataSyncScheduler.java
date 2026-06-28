package com.talex.server.schedulers;

import com.talex.server.dtos.interaction.UserInteractionDto;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.BatchPreparedStatementSetter;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.*;

@Component
public class InteractionDataSyncScheduler {
    private final JdbcTemplate questDbTemplate;
    private final JdbcTemplate postgresTemplate;

    public InteractionDataSyncScheduler(
            @Qualifier("questDbJdbcTemplate") JdbcTemplate questDbTemplate,
            @Qualifier("postgresJdbcTemplate") JdbcTemplate postgresTemplate
    ) {
        this.questDbTemplate = questDbTemplate;
        this.postgresTemplate = postgresTemplate;
    }

    private LocalDateTime lastSyncedTimeUtc = LocalDateTime.now(ZoneOffset.UTC).minusMinutes(5);

    //    @Scheduled(cron = "0 */5 * * * *")
    @Transactional
    public void performSyncFlow() {
        LocalDateTime startLocal = LocalDateTime.of(2026, 6, 28, 8, 0, 0);
        LocalDateTime endLocal = LocalDateTime.of(2026, 6, 28, 10, 0, 0);

//        LocalDateTime endTimeUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime endTimeUtc = endLocal.atZone(ZoneId.systemDefault()) // Gắn múi giờ hiện tại của hệ thống
                .withZoneSameInstant(ZoneOffset.UTC) // Chuyển sang múi giờ UTC
                .toLocalDateTime();

        lastSyncedTimeUtc = startLocal.atZone(ZoneId.systemDefault()) // Gắn múi giờ hiện tại của hệ thống
                .withZoneSameInstant(ZoneOffset.UTC) // Chuyển sang múi giờ UTC
                .toLocalDateTime();

        // PHẦN 1: ĐỒNG BỘ TRẠNG THÁI CHI TIẾT USER (ACCOUNT INTERACTION)
        syncAccountInteractions(lastSyncedTimeUtc, endTimeUtc);

        // PHẦN 2: ĐỒNG BỘ SỐ LIỆU TỔNG HỢP VÀ TIẾN ĐỘ ĐỢT CAMPAIGN
//        syncCampaignMetrics(lastSyncedTimeUtc, endTimeUtc);

        // Đánh dấu đã đồng bộ thành công mốc thời gian này
        this.lastSyncedTimeUtc = endTimeUtc;
    }

    private void syncAccountInteractions(LocalDateTime start, LocalDateTime end) {
        String questDbSql =
                "SELECT account_id, episode_id, interaction_type, interaction_group, timestamp " +
                        "FROM (" +
                        "  SELECT account_id, episode_id, interaction_type, timestamp, " +
                        "    CASE " +
                        "      WHEN interaction_type IN ('LIKE', 'UNLIKE') THEN 'LIKE_GROUP' " +
                        "      WHEN interaction_type IN ('BOOKMARK', 'UNBOOKMARK') THEN 'BOOKMARK_GROUP' " +
                        "    END AS interaction_group " +
                        "  FROM interaction_logs " +
                        "  WHERE timestamp >= ? AND timestamp < ? " +
                        "    AND interaction_type IN ('LIKE', 'UNLIKE', 'BOOKMARK', 'UNBOOKMARK') " +
                        ") " +
                        "LATEST ON timestamp PARTITION BY account_id, episode_id, interaction_group;";

        Map<String, UserInteractionDto> aggregateMap = new HashMap<>();

        // SỬA: Thay đổi vị trí Callback và ép kiểu Timestamp chuẩn UTC
        questDbTemplate.query(
                questDbSql,
                rs -> {
                    String accountId = rs.getString("account_id");
                    String episodeId = rs.getString("episode_id");
                    String group = rs.getString("interaction_group");
                    String type = rs.getString("interaction_type");

                    String key = accountId + "::" + episodeId;
                    UserInteractionDto dto = aggregateMap.computeIfAbsent(key, k -> new UserInteractionDto(accountId, episodeId));

                    if ("LIKE_GROUP".equals(group)) {
                        dto.setHasLikeChange(true);
                        dto.setLike("LIKE".equals(type)); // Đúng: LIKE -> true, UNLIKE -> false
                    } else if ("BOOKMARK_GROUP".equals(group)) {
                        dto.setHasBookmarkChange(true);
                        dto.setBookmark("BOOKMARK".equals(type)); // Đúng: BOOKMARK -> true, UNBOOKMARK -> false
                    }
                },
                Timestamp.from(start.toInstant(ZoneOffset.UTC)),
                Timestamp.from(end.toInstant(ZoneOffset.UTC))
        );

        if (aggregateMap.isEmpty()) return;

        List<UserInteractionDto> updatesList = new ArrayList<>(aggregateMap.values());
        String postgresSql =
                "INSERT INTO account_interaction (id, account_id, episode_id, is_like, is_bookmark, created_at, updated_at) " +
                        "VALUES (?, ?::uuid, ?, ?, ?, NOW(), NOW()) " +
                        "ON CONFLICT (account_id, episode_id) " +
                        "DO UPDATE SET " +
                        "  is_like = CASE WHEN ? = TRUE THEN EXCLUDED.is_like ELSE account_interaction.is_like END, " +
                        "  is_bookmark = CASE WHEN ? = TRUE THEN EXCLUDED.is_bookmark ELSE account_interaction.is_bookmark END, " +
                        "  updated_at = NOW();";

        int batchSize = 500;
        for (int i = 0; i < updatesList.size(); i += batchSize) {
            List<UserInteractionDto> chunk = updatesList.subList(i, Math.min(i + batchSize, updatesList.size()));
            postgresTemplate.batchUpdate(postgresSql, new BatchPreparedStatementSetter() {
                @Override
                public void setValues(PreparedStatement ps, int i) throws SQLException {
                    UserInteractionDto item = chunk.get(i);
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setString(2, item.getAccountId());
                    ps.setString(3, item.getEpisodeId());
                    ps.setBoolean(4, item.isLike());
                    ps.setBoolean(5, item.isBookmark());
                    ps.setBoolean(6, item.isHasLikeChange());
                    ps.setBoolean(7, item.isHasBookmarkChange());
                }

                @Override
                public int getBatchSize() {
                    return chunk.size();
                }
            });
        }
    }
}
