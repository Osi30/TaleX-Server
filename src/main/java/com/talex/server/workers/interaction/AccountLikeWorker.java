package com.talex.server.workers.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.EpisodeHourKey;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.aggregation.LikeAggregationRepository;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
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
public class AccountLikeWorker {
    private final Sender questDBSender;
    private final ObjectMapper objectMapper;
    private final LikeAggregationRepository aggregationRepository;

//    @KafkaListener(
//            topics = "talex-cdc.public.account_likes",
//            groupId = "talex-like-questdb-group",
//            containerFactory = "batchFactory"
//    )
    public void processLikesForQuestDB(List<String> messages) {
        boolean hasData = false;

        try {
            for (String message : messages) {
                if (message == null || message.trim().isEmpty()) continue;

                JsonNode rootNode = objectMapper.readTree(message);
                if (rootNode.get("op") == null) continue;

                String op = rootNode.get("op").asText();
                if ("r".equals(op)) continue; // Bỏ qua snapshot dữ liệu cũ

                JsonNode targetNode = "d".equals(op) ? rootNode.get("before") : rootNode.get("after");
                if (targetNode == null || targetNode.isNull()) continue;

                String likeId = targetNode.has("account_like_id") ? targetNode.get("account_like_id").asText() : null;
                String accountId = targetNode.has("account_id") ? targetNode.get("account_id").asText() : null;
                String episodeId = targetNode.has("episode_id") ? targetNode.get("episode_id").asText() : null;
                long createdAtUs = targetNode.has("created_at") ? targetNode.get("created_at").asLong() : System.currentTimeMillis() * 1000;

                String actionType = "d".equals(op) ? "UNLIKE" : "LIKE";

                if (likeId != null && episodeId != null) {
                    questDBSender.table("like_logs")
                            .symbol("like_id", likeId)
                            .symbol("action_type", actionType)
                            .symbol("account_id", accountId != null ? accountId : "ANONYMOUS")
                            .symbol("episode_id", episodeId)
                            .timestampColumn("created_at", Instant.ofEpochMilli(createdAtUs / 1000))
                            .at(Instant.now());
                    hasData = true;
                }
            }

            if (hasData) {
                questDBSender.flush();
            }

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "QuestDB storage failure: " + e.getMessage());
        }
    }

//    @KafkaListener(
//            topics = "talex-cdc.public.account_likes",
//            groupId = "talex-like-postgres-group",
//            containerFactory = "batchFactory"
//    )
    @Transactional
    public void processLikesForPostgreSQL(List<String> messages) {
        Map<String, Integer> episodeDeltaMap = new HashMap<>();
        Map<EpisodeHourKey, Integer> logDeltaMap = new HashMap<>();

        try {
            for (String message : messages) {
                if (message == null || message.trim().isEmpty()) continue;

                JsonNode rootNode = objectMapper.readTree(message);
                if (rootNode.get("op") == null) continue;

                String op = rootNode.get("op").asText();
                if ("r".equals(op)) continue;

                JsonNode targetNode = "d".equals(op) ? rootNode.get("before") : rootNode.get("after");
                if (targetNode == null || targetNode.isNull()) continue;

                String episodeId = targetNode.has("episode_id") ? targetNode.get("episode_id").asText() : null;
                long createdAtUs = targetNode.has("created_at") ? targetNode.get("created_at").asLong() : System.currentTimeMillis() * 1000;

                // Quy ước: Xóa cứng (d) = giảm 1 (-1), Tạo mới (c) = tăng 1 (+1)
                int delta = "d".equals(op) ? -1 : 1;

                if (episodeId != null) {
                    // Tích lũy Delta cho thực thể chính
                    episodeDeltaMap.put(episodeId, episodeDeltaMap.getOrDefault(episodeId, 0) + delta);

                    // Tích lũy Delta cho Hour Bucket (Làm tròn thời gian về đầu giờ)
                    LocalDateTime ldt = LocalDateTime.ofInstant(Instant.ofEpochMilli(createdAtUs / 1000), ZoneId.systemDefault());
                    LocalDateTime hourBucket = ldt.truncatedTo(ChronoUnit.HOURS);

                    EpisodeHourKey key = new EpisodeHourKey(episodeId, hourBucket);
                    logDeltaMap.put(key, logDeltaMap.getOrDefault(key, 0) + delta);
                }
            }

            // Thực thi cập nhật dồn tích xuống các bảng chính (Episode, Series, Campaign, Creator)
            episodeDeltaMap.forEach((episodeId, totalDelta) -> {
                if (totalDelta != 0) {
                    aggregationRepository.updateEpisodeLikeCount(episodeId, totalDelta);
                    aggregationRepository.updateSeriesLikeCountByEpisode(episodeId, totalDelta);
                    aggregationRepository.updateCampaignEpisodeLikeCount(episodeId, totalDelta);
                    aggregationRepository.updateCampaignLikeCountAndTarget(episodeId, totalDelta);
                    aggregationRepository.updateCreatorLikeCount(episodeId, totalDelta);
                }
            });

            // Thực thi Upsert dồn tích xuống các bảng Log theo khung giờ công việc (Mục 4)
            logDeltaMap.forEach((key, totalDelta) -> {
                if (totalDelta != 0) {
                    aggregationRepository.upsertEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertSeriesLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCampaignEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCampaignLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                }
            });

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "PostgreSQL aggregation failure: " + e.getMessage());
        }
    }
}