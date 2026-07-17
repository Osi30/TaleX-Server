package com.talex.server.services.impls;

import com.talex.server.services.RecommendationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Collections;
import java.util.List;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final StringRedisTemplate redisTemplate;
    private final JdbcTemplate questDbJdbcTemplate;

    public RecommendationServiceImpl(
            StringRedisTemplate redisTemplate,
            @Qualifier("questDbJdbcTemplate") JdbcTemplate questDbJdbcTemplate
    ) {
        this.redisTemplate = redisTemplate;
        this.questDbJdbcTemplate = questDbJdbcTemplate;
    }
    
    private static final String REDIS_KEY_PREFIX = "watch:top5:recent_series:";
    private static final Duration CACHE_TTL = Duration.ofDays(1);

    @Override
    public List<String> getRecentWatchedSeries(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String redisKey = REDIS_KEY_PREFIX + accountId;

        try {
            // 1. THỬ LẤY TỪ REDIS CACHE (Lấy từ index 0 đến 4)
            List<String> cachedSeries = redisTemplate.opsForList().range(redisKey, 0, 4);

            // 1.1 CÓ CACHE
            if (cachedSeries != null && !cachedSeries.isEmpty()) {
                return cachedSeries;
            }

            // 1.2. CACHE MISS -> TRUY VẤN QUESTDB
            List<String> dbSeries = fetchRecentSeriesFromQuestDB(accountId);

            // 3. NẾU CÓ DỮ LIỆU -> REBUILD LẠI CACHE TRÊN REDIS
            if (!dbSeries.isEmpty()) {
                // Xóa key cũ phòng trường hợp có rác/dữ liệu lệch
                redisTemplate.delete(redisKey);
                redisTemplate.opsForList().rightPushAll(redisKey, dbSeries);
                redisTemplate.expire(redisKey, CACHE_TTL);
            }

            return dbSeries;

        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách series gần nhất cho user {}: {}", accountId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /**
     * Hàm helper thực thi query trực tiếp trên QuestDB qua JDBC
     */
    private List<String> fetchRecentSeriesFromQuestDB(String accountId) {
        String sql = "SELECT series_id FROM watch_session_logs " +
                "WHERE account_id = ? AND series_id IS NOT NULL " +
                "LATEST ON timestamp PARTITION BY series_id " +
                "ORDER BY timestamp DESC " +
                "LIMIT 5";

        try {
            return questDbJdbcTemplate.query(
                    sql, (rs, rowNum) -> rs.getString("series_id"), accountId);
        } catch (Exception e) {
            log.error("Thất bại khi query QuestDB cho accountId {}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
