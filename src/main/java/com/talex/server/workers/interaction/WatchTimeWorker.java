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
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
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

    @KafkaListener(
            topics = "talex-cdc.public.watch_session",
            groupId = "talex-watch-questdb-group",
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

//    @KafkaListener(
//            topics = "talex-cdc.public.watch_session",
//            groupId = "talex-watch-cdc-stats-group",
//            containerFactory = "batchFactory"
//    )
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
                watchTimeAggregationRepository.updateSeriesWatchTime(seriesId, totalDelta);
                watchTimeAggregationRepository.updateCampaignSeriesWatchTime(seriesId, totalDelta);
                watchTimeAggregationRepository.updateCampaignWatchTimeAndTarget(seriesId, totalDelta);
                watchTimeAggregationRepository.updateCreatorWatchTime(seriesId, totalDelta);
            });

            logWatchTimeDeltaMap.forEach((key, totalDelta) -> {
                String seriesId = episodeService.getSeriesIdByEpisodeId(key.getEpisodeId());
                watchTimeAggregationRepository.upsertEpisodeLogWatchTime(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertCampaignSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertCampaignLog(seriesId, key.getHourBucket(), totalDelta);
                watchTimeAggregationRepository.upsertCreatorLogWatchTime(seriesId, key.getHourBucket(), totalDelta);
            });
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Thất bại khi thực thi ghi Batch Stats dựa trên dữ liệu CDC sạch: " + e.getMessage());
        }
    }

    private double getDelta(JsonNode before, JsonNode after) {
        double afterDuration = after.has("watch_duration") ? after.get("watch_duration").asDouble() : 0.0;
        double beforeDuration = (before != null && !before.isNull() && before.has("watch_duration"))
                ? before.get("watch_duration").asDouble()
                : 0.0;
        double delta = afterDuration - beforeDuration;
        return Math.max(0.0, delta);
    }
}
