package com.talex.server.workers.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.EpisodeHourKey;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.WatchSessionRepository;
import com.talex.server.repositories.interaction.aggregation.WatchTimeAggregationRepository;
import com.talex.server.services.series.EpisodeService;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class WatchTimeWorker {
    private final ObjectMapper objectMapper;
    private final Sender questDBSender;
    private final EpisodeService episodeService;
    private final WatchSessionRepository watchSessionRepository;
    private final WatchTimeAggregationRepository watchTimeAggregationRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PREFIX = "watch:top5:recent_series:";

    /// Gửi log thô cho QuestDB
    @KafkaListener(
            topics = "talex-cdc.public.watch_session",
            groupId = "talex-watch-questdb-group-local",
            containerFactory = "batchFactory"
    )
    public void processWatchProgressForQuestDB(List<String> messages) {
        try {
            for (String message : messages) {
                JsonNode cdcPayload = objectMapper.readTree(message);
                if (cdcPayload == null) continue;

                String op = cdcPayload.get("op").asText();
                if (!"u".equals(op)) {
                    continue;
                }

                JsonNode after = cdcPayload.get("after");
                JsonNode before = cdcPayload.get("before");

                if (after == null) continue;

                // Tính toán lượng watch time tăng thêm bằng cách lấy after - before
                double heartbeatValue = getDelta(before, after);

                // Nếu không có lượng xem tăng thêm thực tế (ví dụ: client gửi spam trùng thời gian), bỏ qua không ghi log
                if (heartbeatValue <= 0) {
                    continue;
                }

                // Trích xuất các trường từ payload CDC (tên cột trong Postgres thường là snake_case)
                String sessionId = after.get("watch_session_id").asText();
                String episodeId = after.get("episode_id").asText();
                String seriesId = episodeService.getSeriesIdByEpisodeId(episodeId);

                // account_id có thể mang giá trị null nếu người dùng xem ẩn danh
                String accountId = (after.has("account_id") && !after.get("account_id").isNull())
                        ? after.get("account_id").asText()
                        : "anonymous";

                double currentPosition = after.has("current_position") ? after.get("current_position").asDouble() : 0.0;

                // Lấy mốc thời gian từ cột end_time (Debezium convert sang Microseconds)
                long endTimeUs = after.get("end_time").asLong();
                long endTimeMs = endTimeUs / 1000;
                LocalDateTime localDateTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeMs), ZoneOffset.UTC);
                Instant instantTimestamp = localDateTime.atZone(ZoneId.systemDefault()).toInstant();

                // Đẩy dữ liệu log thô vào QuestDB để phân tích thời gian thực
                questDBSender.table("watch_session_logs")
                        .symbol("session_id", sessionId)
                        .symbol("episode_id", episodeId)
                        .symbol("series_id", seriesId)
                        .symbol("account_id", accountId)
                        .doubleColumn("current_position", currentPosition)
                        .doubleColumn("watch_time", heartbeatValue)
                        .at(instantTimestamp);
            }
            questDBSender.flush();
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "QuestDB CDC worker aggregation error: " + e.getMessage());
        }
    }

    /// Lưu trữ trực tiếp vào PostgreSQL
    @KafkaListener(
            topics = "watch-raw",
            groupId = "talex-watch-session-entity-group-local",
            containerFactory = "batchFactory"
    )
    @Transactional
    public void processWatchSessionUpdates(List<String> messages) {
        for (String message : messages) {
            try {
                JsonNode eventNode = objectMapper.readTree(message);
                if (eventNode == null) continue;

                String sessionId = eventNode.get("session_id").asText();
                String episodeId = eventNode.get("episode_id").asText();
                double currentPosition = eventNode.get("current_position").asDouble();
                double heartbeatValue = eventNode.get("heartbeat_value").asDouble();
                long tsMs = eventNode.get("timestamp").asLong();

                LocalDateTime heartbeatTime = LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMs), ZoneId.systemDefault());

                int updatedRows = watchSessionRepository.updateSession(
                        sessionId, episodeId, currentPosition, heartbeatValue, heartbeatTime
                );

                if (updatedRows == 0) {
                    log.warn("Heartbeat bị loại bỏ bởi DB chống gian lận (Spam/Gửi ngược/Sai lệch delta): Session {}", sessionId);
                }

            } catch (IllegalArgumentException ex) {
                throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Hủy bản ghi do sai định dạng UUID của accountId trong message: " + ex.getMessage());
            } catch (Exception e) {
                throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Lỗi xử lý cập nhật thực thể WatchSession chi tiết cho bản tin: " + e.getMessage());
            }
        }
    }

    /// Cập nhập stat cho các bảng liên quan
    @KafkaListener(
            topics = "talex-cdc.public.watch_session",
            groupId = "talex-watch-cdc-stats-group",
            containerFactory = "batchFactory"
    )
    @Transactional
    public void processWatchSessionCDCEvents(List<String> messages) {
        Map<String, Double> globalWatchTimeDeltaMap = new HashMap<>();
        Map<EpisodeHourKey, Double> logWatchTimeDeltaMap = new HashMap<>();

        for (String message : messages) {
            try {
                JsonNode cdcPayload = objectMapper.readTree(message);
                if (cdcPayload == null) continue;

                String op = cdcPayload.get("op").asText();
                if (!"u".equals(op)) {
                    continue;
                }

                JsonNode after = cdcPayload.get("after");
                JsonNode before = cdcPayload.get("before");

                if (after == null) continue;

                String episodeId = after.get("episode_id").asText();
                double delta = getDelta(before, after);

                // Tích lũy delta cho biểu đồ tổng quan
                globalWatchTimeDeltaMap.put(episodeId, globalWatchTimeDeltaMap.getOrDefault(episodeId, 0.0) + delta);

                // Lấy mốc thời gian cập nhật cuối cùng (end_time) từ bản ghi sau khi update để chia Hour Bucket
                long endTimeUs = after.get("end_time").asLong();
                long endTimeMs = endTimeUs / 1000;
                LocalDateTime hourBucket = LocalDateTime.ofInstant(Instant.ofEpochMilli(endTimeMs), ZoneId.of("UTC"))
                        .truncatedTo(ChronoUnit.HOURS);

                EpisodeHourKey hourKey = new EpisodeHourKey(episodeId, hourBucket);
                logWatchTimeDeltaMap.put(hourKey, logWatchTimeDeltaMap.getOrDefault(hourKey, 0.0) + delta);

            } catch (Exception e) {
                throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Lỗi phân rã cấu trúc bản tin Kafka CDC: " + e.getMessage());
            }
        }

        if (globalWatchTimeDeltaMap.isEmpty() && logWatchTimeDeltaMap.isEmpty()) return;

        try {
            globalWatchTimeDeltaMap.forEach((episodeId, totalDelta) -> {
                String seriesId = episodeService.getSeriesIdByEpisodeId(episodeId);
                watchTimeAggregationRepository.updateEpisodeWatchTime(episodeId, totalDelta);
                watchTimeAggregationRepository.updateSeriesWatchTime(seriesId, totalDelta, LocalDateTime.now());
                watchTimeAggregationRepository.updateCreatorWatchTime(seriesId, totalDelta);
                int updatedRow = watchTimeAggregationRepository.updateCampaignSeriesWatchTime(seriesId, totalDelta);
                if (updatedRow > 0) watchTimeAggregationRepository.updateCampaignWatchTimeAndTarget(seriesId, totalDelta);
            });

            logWatchTimeDeltaMap.forEach((key, totalDelta) -> {
                String seriesId = episodeService.getSeriesIdByEpisodeId(key.getEpisodeId());
                watchTimeAggregationRepository.upsertEpisodeLogWatchTime(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertCampaignSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertCreatorLogWatchTime(seriesId, key.getHourBucket(), totalDelta);
            });
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Thất bại khi thực thi ghi Batch Stats dựa trên dữ liệu CDC sạch: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "talex-cdc.public.watch_session",
            groupId = "talex-watch-redis-top5-group",
            containerFactory = "batchFactory"
    )
    public void processInitialWatchEventsForRedis(List<String> messages) {
        for (String message : messages) {
            try {
                JsonNode cdcPayload = objectMapper.readTree(message);
                if (cdcPayload == null) continue;

                // Chỉ xử lý các sự kiện UPDATE từ Debezium CDC
                String op = cdcPayload.get("op").asText();
                if (!"u".equals(op)) {
                    continue;
                }

                JsonNode after = cdcPayload.get("after");
                JsonNode before = cdcPayload.get("before");

                if (after == null || before == null) continue;

                // Nếu account null hoặc không tồn tại thì bỏ qua ngay lập tức
                if (!after.has("account_id") || after.get("account_id").isNull()) {
                    continue;
                }
                String accountId = after.get("account_id").asText();

                // Trích xuất watch_duration của before và after
                double beforeDuration = !before.isNull() && before.has("watch_duration")
                        ? before.get("watch_duration").asDouble()
                        : 0.0;
                double afterDuration = after.has("watch_duration") ? after.get("watch_duration").asDouble() : 0.0;

                // Sự kiện ban đầu (từ 0.0 tăng lên đúng 5.0 giây)
                if (beforeDuration == 0.0 && afterDuration == 5.0) {
                    String episodeId = after.get("episode_id").asText();
                    String seriesId = episodeService.getSeriesIdByEpisodeId(episodeId);

                    // Thực hiện ghi nhận top 5 series cho user này
                    updateTop5SeriesInRedis(accountId, seriesId);
                }

            } catch (Exception e) {
                log.error("Lỗi xử lý luồng ghi Redis Top 5: {}", e.getMessage(), e);
            }
        }
    }

    /// Lấy khoảng cập nhập duration từ before sang after
    private double getDelta(JsonNode before, JsonNode after) {
        double afterDuration = after.has("watch_duration") ? after.get("watch_duration").asDouble() : 0.0;
        double beforeDuration = (before != null && !before.isNull() && before.has("watch_duration"))
                ? before.get("watch_duration").asDouble()
                : 0.0;
        double delta = afterDuration - beforeDuration;
        return Math.max(0.0, delta);
    }

    /**
     * Cập nhật danh sách Top 5 hoạt động gần nhất của một User cụ thể lên Redis.
     * Cấu trúc Key: watch:top5:recent_series:{accountId}
     */
    private void updateTop5SeriesInRedis(String accountId, String seriesId) {
        String userRedisKey = REDIS_KEY_PREFIX + accountId;

        // 1. Lấy danh sách Top 5 hiện tại của user này trên Redis (từ index 0 đến 4)
        List<String> currentTop5 = redisTemplate.opsForList().range(userRedisKey, 0, 4);

        // 2. Nếu danh sách trống hoặc chưa chứa seriesId này
        if (currentTop5 == null || !currentTop5.contains(seriesId)) {

            // Đẩy phần tử mới vào đầu danh sách (Left Push)
            redisTemplate.opsForList().leftPush(userRedisKey, seriesId);

            // Cắt tỉa danh sách, chỉ giữ lại index từ 0 đến 4 (tổng cộng 5 phần tử mới nhất)
            redisTemplate.opsForList().trim(userRedisKey, 0, 4);
            redisTemplate.expire(userRedisKey, Duration.ofDays(1));

            log.info("Đã thêm series {} vào Top 5 của user: {}", seriesId, accountId);
        } else {
            log.debug("Series {} đã tồn tại trong Top 5 của user {}, bỏ qua không cập nhật.", seriesId, accountId);
        }
    }
}
