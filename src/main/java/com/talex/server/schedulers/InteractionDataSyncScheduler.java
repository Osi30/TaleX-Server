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
        LocalDateTime startLocal = LocalDateTime.of(2026, 6, 28, 16, 11, 0);
        LocalDateTime endLocal = LocalDateTime.of(2026, 6, 28, 17, 13, 0);

//        LocalDateTime endTimeUtc = LocalDateTime.now(ZoneOffset.UTC);
        LocalDateTime endTimeUtc = endLocal.atZone(ZoneId.systemDefault()) // Gắn múi giờ hiện tại của hệ thống
                .withZoneSameInstant(ZoneOffset.UTC) // Chuyển sang múi giờ UTC
                .toLocalDateTime();

        lastSyncedTimeUtc = startLocal.atZone(ZoneId.systemDefault()) // Gắn múi giờ hiện tại của hệ thống
                .withZoneSameInstant(ZoneOffset.UTC) // Chuyển sang múi giờ UTC
                .toLocalDateTime();

        // PHẦN 2: ĐỒNG BỘ SỐ LIỆU TỔNG HỢP VÀ TIẾN ĐỘ ĐỢT CAMPAIGN
//        syncCampaignMetrics(lastSyncedTimeUtc, endTimeUtc);

        // Đánh dấu đã đồng bộ thành công mốc thời gian này
        this.lastSyncedTimeUtc = endTimeUtc;
    }
}
