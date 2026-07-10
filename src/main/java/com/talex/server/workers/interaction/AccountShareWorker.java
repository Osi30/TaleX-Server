package com.talex.server.workers.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.EpisodeHourKey;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.aggregation.ShareAggregationRepository;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
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

@Component
@RequiredArgsConstructor
public class AccountShareWorker {
    private final Sender questDBSender;
    private final ObjectMapper objectMapper;
    private final ShareAggregationRepository shareAggregationRepository;

    @KafkaListener(
            topics = "talex-interaction.episode-shared",
            groupId = "talex-share-questdb-group",
            containerFactory = "batchFactory"
    )
    public void processSharesForQuestDB(List<String> messages) {
        try {
            for (String message : messages) {
                // Đọc trực tiếp Json phẳng từ Service truyền sang
                JsonNode eventNode = objectMapper.readTree(message);
                if (eventNode == null) continue;

                String episodeId = eventNode.get("episode_id").asText();
                String accountId = eventNode.get("account_id").asText();
                long tsMs = eventNode.get("timestamp").asLong();

                // Bọc thời gian bằng thực thể Instant gửi trực tiếp qua QuestDB
                Instant instantTimestamp = Instant.ofEpochMilli(tsMs);

                questDBSender.table("share_logs")
                        .symbol("episode_id", episodeId)
                        .symbol("account_id", accountId)
                        .longColumn("delta", 1L) // Mặc định luôn là tăng 1 đơn vị
                        .at(instantTimestamp);
            }
            questDBSender.flush();
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Share PostgreSQL worker aggregation error: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "talex-interaction.episode-shared",
            groupId = "talex-share-postgres-group",
            containerFactory = "batchFactory"
    )
    @Transactional
    public void processSharesForPostgreSQL(List<String> messages) {
        Map<String, Long> episodeDeltaMap = new HashMap<>();
        Map<EpisodeHourKey, Long> logDeltaMap = new HashMap<>();

        try {
            for (String message : messages) {
                JsonNode eventNode = objectMapper.readTree(message);
                if (eventNode == null) continue;

                String episodeId = eventNode.get("episode_id").asText();
                long tsMs = eventNode.get("timestamp").asLong();

                long delta = 1L; // Cố định tăng tịnh tiến 1

                // 1. Gom nhóm tổng số lượng share tăng thêm theo EpisodeId
                episodeDeltaMap.put(episodeId, episodeDeltaMap.getOrDefault(episodeId, 0L) + delta);

                // 2. Gom nhóm tổng số lượng tăng thêm theo mốc giờ (Hour Bucket)
                LocalDateTime hourBucket = LocalDateTime.ofInstant(Instant.ofEpochMilli(tsMs), ZoneId.of("UTC"))
                        .truncatedTo(ChronoUnit.HOURS);

                EpisodeHourKey hourKey = new EpisodeHourKey(episodeId, hourBucket);
                logDeltaMap.put(hourKey, logDeltaMap.getOrDefault(hourKey, 0L) + delta);
            }

            // --- THỰC THI GHI BATCH DỮ LIỆU TỔNG HỢP XUỐNG POSTGRESQL ---

            // Cập nhật các bảng tổng số lượng lũy kế tổng thể
            episodeDeltaMap.forEach((episodeId, totalDelta) -> {
                if (totalDelta > 0) {
                    shareAggregationRepository.updateEpisodeShareCount(episodeId, totalDelta);
                    shareAggregationRepository.updateSeriesShareCountByEpisode(episodeId, totalDelta);
                    shareAggregationRepository.updateCampaignEpisodeShareCount(episodeId, totalDelta);
                    shareAggregationRepository.updateCampaignShareCountAndTarget(episodeId, totalDelta);
                    shareAggregationRepository.updateCreatorShareCount(episodeId, totalDelta);
                }
            });

            // Cập nhật dữ liệu Log giờ giấc bằng cấu trúc Native SQL Upsert an toàn (tránh NULL)
            logDeltaMap.forEach((key, totalDelta) -> {
                if (totalDelta > 0) {
                    shareAggregationRepository.upsertEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    shareAggregationRepository.upsertSeriesLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    shareAggregationRepository.upsertCampaignEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    shareAggregationRepository.upsertCampaignLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                }
            });

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Direct Share PostgreSQL worker aggregation error: " + e.getMessage());
        }
    }
}