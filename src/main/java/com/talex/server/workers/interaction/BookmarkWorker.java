package com.talex.server.workers.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.EpisodeHourKey;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.aggregation.BookmarkAggregationRepository;
import com.talex.server.services.EpisodeService;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class BookmarkWorker {
    private final Sender questDBSender;
    private final ObjectMapper objectMapper;
    private final EpisodeService episodeService;
    private final BookmarkAggregationRepository aggregationRepository;


    @KafkaListener(
            topics = "talex-cdc.public.account_bookmarks",
            groupId = "talex-bookmark-questdb-group",
            containerFactory = "batchFactory"
    )
    public void processBookmarksForQuestDB(List<String> messages) {
        try {
            for (String message : messages) {
                if (message == null || message.trim().isEmpty()) continue;

                JsonNode rootNode = objectMapper.readTree(message);
                String op = rootNode.get("op").asText();
                if ("r".equals(op)) {
                    continue; // Bỏ qua snapshot ban đầu nếu có
                }

                JsonNode targetNode = getTargetNode(rootNode);
                if (targetNode == null || targetNode.isNull()) {
                    continue;
                }

                String bookmarkId = targetNode.has("bookmark_id") ? targetNode.get("bookmark_id").asText() : null;
                String accountId = targetNode.has("account_id") ? targetNode.get("account_id").asText() : null;
                String episodeId = targetNode.has("episode_id") ? targetNode.get("episode_id").asText() : null;
                long createdAt = targetNode.has("created_at") ? targetNode.get("created_at").asLong() : Instant.now().toEpochMilli() * 1000;

                // Ghi dữ liệu dạng Time-series vào QuestDB
                questDBSender.table("bookmark_logs")
                        .symbol("bookmark_id", bookmarkId)
                        .symbol("action_type", op) // 'c' đại diện cho BOOKMARK, 'd' cho UNBOOKMARK
                        .symbol("account_id", accountId)
                        .symbol("episode_id", episodeId)
                        .timestampColumn("created_at", Instant.ofEpochMilli(createdAt / 1000))
                        .at(Instant.now());
            }
            questDBSender.flush();
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "QuestDB processing failure: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "talex-cdc.public.account_bookmarks",
            groupId = "talex-bookmark-postgres-group",
            containerFactory = "batchFactory"
    )
    public void processBookmarksForPostgres(List<String> messages) {
        Map<String, Long> episodeDeltaMap = new HashMap<>();
        Map<EpisodeHourKey, Long> logDeltaMap = new HashMap<>();

        try {
            for (String message : messages) {
                if (message == null || message.trim().isEmpty()) continue;

                JsonNode rootNode = objectMapper.readTree(message);
                String op = rootNode.get("op").asText();
                if ("r".equals(op)) {
                    continue;
                }

                JsonNode targetNode = getTargetNode(rootNode);
                if (targetNode == null || targetNode.isNull()) {
                    continue;
                }

                String episodeId = targetNode.get("episode_id").asText();
                long createdAt = targetNode.has("created_at") ? targetNode.get("created_at").asLong() : Instant.now().toEpochMilli() * 1000;

                // Định dạng localdatetime gom cụm theo giờ (Hour Bucket)
                LocalDateTime hourBucket = LocalDateTime.ofInstant(
                        Instant.ofEpochMilli(createdAt / 1000),
                        ZoneId.systemDefault()
                ).truncatedTo(ChronoUnit.HOURS);

                // Xác định delta tăng giảm: 'd' (delete) -> -1, 'c' (create) -> +1
                long delta = "d".equals(op) ? -1L : 1L;

                episodeDeltaMap.put(episodeId, episodeDeltaMap.getOrDefault(episodeId, 0L) + delta);

                EpisodeHourKey key = new EpisodeHourKey(episodeId, hourBucket);
                logDeltaMap.put(key, logDeltaMap.getOrDefault(key, 0L) + delta);
            }

            // 1. Đồng bộ số lượng tổng hợp thực thể (Mục 3 & 5)
            episodeDeltaMap.forEach((episodeId, totalDelta) -> {
                if (totalDelta != 0) {
                    String seriesId = episodeService.getSeriesIdByEpisodeId(episodeId);
                    aggregationRepository.updateEpisodeBookmarkCount(episodeId, totalDelta);
                    aggregationRepository.updateSeriesBookmarkCount(seriesId, totalDelta);
                    aggregationRepository.updateCampaignSeriesBookmarkCount(seriesId, totalDelta);
                    aggregationRepository.updateCampaignBookmarkCountAndTarget(seriesId, totalDelta);
                    aggregationRepository.updateCreatorBookmarkCount(seriesId, totalDelta);
                }
            });

            // 2. Cập nhật các bảng Log theo Hour Bucket (Mục 4) bằng Upsert Native SQL
            logDeltaMap.forEach((key, totalDelta) -> {
                if (totalDelta != 0) {
                    String seriesId = episodeService.getSeriesIdByEpisodeId(key.getEpisodeId());
                    aggregationRepository.upsertEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCampaignSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCampaignLog(seriesId, key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCreatorLogBookmarks(seriesId, key.getHourBucket(), totalDelta);
                }
            });

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "PostgreSQL aggregation failure: " + e.getMessage());
        }
    }

    private JsonNode getTargetNode(JsonNode rootNode) {
        JsonNode targetNode = rootNode.get("after");
        if (targetNode == null || targetNode.isNull()) {
            targetNode = rootNode.get("before");
        }
        return targetNode;
    }
}