package com.talex.server.workers.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.EpisodeHourKey;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.WatchSessionRepository;
import com.talex.server.repositories.interaction.aggregation.ViewAggregationRepository;
import com.talex.server.services.EpisodeService;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class EpisodeViewWorker {
    private final Sender questDBSender;
    private final ObjectMapper objectMapper;
    private final EpisodeService episodeService;
    private final ViewAggregationRepository viewAggregationRepository;
    private final WatchSessionRepository watchSessionRepository;

    @KafkaListener(
            topics = "talex-interaction.episode-viewed",
            groupId = "talex-view-questdb-group-local",
            containerFactory = "batchFactory"
    )
    public void processViewsForQuestDB(List<String> messages) {
        try {
            for (String message : messages) {
                JsonNode eventNode = objectMapper.readTree(message);
                if (eventNode == null) continue;

                String episodeId = eventNode.get("episode_id").asText();
                String ipAddress = eventNode.get("ip_address").asText();
                String sessionId = eventNode.get("session_id").asText();
                String accountId = eventNode.get("account_id").asText();
                long tsMs = eventNode.get("timestamp").asLong();

                Instant instantTimestamp = Instant.ofEpochMilli(tsMs);

                questDBSender.table("view_logs")
                        .symbol("episode_id", episodeId)
                        .symbol("ip_address", ipAddress)
                        .symbol("session_id", sessionId)
                        .symbol("account_id", accountId)
                        .longColumn("delta", 1L)
                        .at(instantTimestamp);
            }
            questDBSender.flush();
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Share PostgreSQL worker aggregation error: " + e.getMessage());
        }
    }

//    @KafkaListener(
//            topics = "talex-interaction.episode-viewed",
//            groupId = "talex-view-postgres-group",
//            containerFactory = "batchFactory"
//    )
    @Transactional
    public void processViewsForPostgreSQL(List<String> messages) {
        Map<String, Long> episodeDeltaMap = new HashMap<>();
        Map<EpisodeHourKey, Long> logDeltaMap = new HashMap<>();

        try {
            for (String message : messages) {
                JsonNode eventNode = objectMapper.readTree(message);
                if (eventNode == null) continue;

                String episodeId = eventNode.get("episode_id").asText();
                long tsMs = eventNode.get("timestamp").asLong();

                long delta = 1L;

                // 1. Gom nhóm tổng số lượng view tăng thêm theo EpisodeId
                episodeDeltaMap.put(episodeId, episodeDeltaMap.getOrDefault(episodeId, 0L) + delta);

                // 2. Gom nhóm tổng số lượng tăng thêm theo khung giờ (Hour Bucket)
                LocalDateTime hourBucket = LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMs), ZoneId.systemDefault())
                        .truncatedTo(ChronoUnit.HOURS);

                EpisodeHourKey hourKey = new EpisodeHourKey(episodeId, hourBucket);
                logDeltaMap.put(hourKey, logDeltaMap.getOrDefault(hourKey, 0L) + delta);
            }

            // Cập nhật các bảng lũy kế tổng số lượng view tổng thể
            episodeDeltaMap.forEach((episodeId, totalDelta) -> {
                if (totalDelta > 0) {
                    String seriesId = episodeService.getSeriesIdByEpisodeId(episodeId);
                    viewAggregationRepository.updateEpisodeViewCount(episodeId, totalDelta);
                    viewAggregationRepository.updateSeriesViewCount(seriesId, totalDelta);
                    viewAggregationRepository.updateCampaignSeriesViewCount(seriesId, totalDelta);
                    viewAggregationRepository.updateCampaignViewCountAndTarget(seriesId, totalDelta);
                    viewAggregationRepository.updateCreatorViewCount(seriesId, totalDelta);
                }
            });

            // Cập nhật dữ liệu Log thống kê giờ giấc
            logDeltaMap.forEach((key, totalDelta) -> {
                if (totalDelta > 0) {
                    String seriesId = episodeService.getSeriesIdByEpisodeId(key.getEpisodeId());
                    viewAggregationRepository.upsertEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    viewAggregationRepository.upsertSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                    viewAggregationRepository.upsertCampaignSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                    viewAggregationRepository.upsertCampaignLog(seriesId, key.getHourBucket(), totalDelta);
                    viewAggregationRepository.upsertCreatorLogViews(seriesId, key.getHourBucket(), totalDelta);
                }
            });

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Direct View PostgreSQL worker aggregation error: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "talex-interaction.episode-viewed",
            groupId = "talex-view-postgres-session-init-group-local",
            containerFactory = "batchFactory"
    )
    @Transactional
    public void processViewsForSessionInitialization(List<String> messages) {
        for (String message : messages) {
            try {
                JsonNode eventNode = objectMapper.readTree(message);
                if (eventNode == null) continue;
                String episodeId = eventNode.get("episode_id").asText();
                String watchSessionId = eventNode.get("session_id").asText();
                Instant instant = Instant.ofEpochMilli(eventNode.get("timestamp").asLong());
                LocalDateTime localDateTime = LocalDateTime.ofInstant(instant, ZoneId.systemDefault());

                UUID accountId = null;
                String accountIdStr = eventNode.get("account_id").asText();
                if (!accountIdStr.equals("anonymous")) {
                    accountId = UUID.fromString(accountIdStr);
                }

                watchSessionRepository
                        .initializeDefaultSession(watchSessionId, accountId, episodeId, localDateTime);

            } catch (Exception ex) {
                throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Direct Watch Session PostgreSQL worker aggregation error: " + ex.getMessage());
            }
        }
    }
}