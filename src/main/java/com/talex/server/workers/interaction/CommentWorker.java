package com.talex.server.workers.interaction;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.EpisodeHourKey;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.aggregation.CommentAggregationRepository;
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
import java.time.temporal.ChronoUnit;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Slf4j
public class CommentWorker {
    private final Sender questDBSender;
    private final ObjectMapper objectMapper;
    private final EpisodeService episodeService;
    private final CommentAggregationRepository aggregationRepository;

    @KafkaListener(
            topics = "talex-cdc.public.account_comments",
            groupId = "talex-comment-single-group",
            containerFactory = "singleFactory"
    )
    public void listenAccountComments(String message) {
        try {
            JsonNode rootNode = objectMapper.readTree(message);

            String op = rootNode.get("op").asText();

            // Bỏ qua trường hợp 'r' (Snapshot) hoặc các hành động lạ khác
            if ("r".equals(op)) {
                return;
            }

            JsonNode targetNode = getTargetNode(rootNode);
            if (targetNode == null || targetNode.isNull()) return;

            boolean isHidden = targetNode.has("is_hidden") && targetNode.get("is_hidden").asBoolean();
            String actionType;

            // Phân loại lấy 'after' hoặc 'before' + gán action_type
            switch (op) {
                case "c":
                    actionType = "CREATE";
                    break;
                case "u":
                    if (isHidden) {
                        actionType = "HIDDEN";
                    } else {
                        actionType = "UPDATE";
                    }
                    break;
                default:
                    return;
            }

            // Trích xuất an toàn các trường dữ liệu từ Node đã chọn (after hoặc before)
            String commentId = targetNode.has("comment_id") ? targetNode.get("comment_id").asText() : null;
            String content = targetNode.has("content") ? targetNode.get("content").asText() : null;

            // Đổi timestamp dạng Epoch Microseconds từ Postgres/Debezium về dạng phù hợp
            long createdAt = targetNode.has("created_at") ? targetNode.get("created_at").asLong() : 0L;
            long updatedAt = targetNode.has("updated_at") ? targetNode.get("updated_at").asLong() : 0L;

            String accountId = targetNode.has("account_id") ? targetNode.get("account_id").asText() : null;
            String episodeId = targetNode.has("episode_id") ? targetNode.get("episode_id").asText() : null;

            // Xử lý riêng parent_id vì trường này rất dễ mang giá trị null dữ liệu gốc
            String parentId = (targetNode.has("parent_id") && !targetNode.get("parent_id").isNull())
                    ? targetNode.get("parent_id").asText() : null;

            questDBSender.table("comment_logs")
                    .symbol("comment_id", commentId)
                    .symbol("action_type", actionType)
                    .symbol("account_id", accountId)
                    .symbol("episode_id", episodeId)
                    .symbol("parent_id", parentId != null ? parentId : "NONE")
                    .stringColumn("content", content)
                    .timestampColumn("created_at", Instant.ofEpochMilli(createdAt / 1000))
                    .boolColumn("is_hidden", isHidden)
                    .timestampColumn("updated_at", Instant.ofEpochMilli(updatedAt / 1000))
                    .at(Instant.now());
            questDBSender.flush();

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Comment worker error: " + e.getMessage());
        }
    }

    @KafkaListener(
            topics = "talex-cdc.public.account_comments",
            groupId = "talex-comment-batch-group",
            containerFactory = "batchFactory"
    )
    @Transactional(rollbackFor = Exception.class)
    public void processComments(List<String> messages) {
        // Map lưu trữ: Key = EpisodeId, Value = Tổng số lượng thay đổi (Delta)
        Map<String, Integer> episodeDeltaMap = new HashMap<>();

        // Map lưu trữ cho Log theo giờ: Key = Kết hợp (EpisodeId + HourBucket), Value = Delta
        Map<EpisodeHourKey, Integer> logDeltaMap = new HashMap<>();

        try {
            for (String message : messages) {
                if (message == null) {
                    continue;
                }
                JsonNode rootNode = objectMapper.readTree(message);
                String op = rootNode.get("op").asText();
                JsonNode targetNode = getTargetNode(rootNode);
                if (targetNode == null || targetNode.isNull()) continue;

                boolean isHidden = targetNode.has("is_hidden") && targetNode.get("is_hidden").asBoolean();

                // Bỏ qua
                if ("r".equals(op) || ("u".equals(op) && !isHidden)) {
                    return;
                }

                int delta = "c".equals(op) ? 1 : -1;


                String episodeId = targetNode.has("episode_id") ? targetNode.get("episode_id").asText() : null;
                long eventTsMs = rootNode.get("source").get("ts_ms").asLong();

                if (episodeId != null) {
                    // Cộng dồn Delta cho Episode chính trên RAM
                    episodeDeltaMap.put(episodeId, episodeDeltaMap.getOrDefault(episodeId, 0) + delta);

                    // Làm tròn thời gian về đầu giờ gần nhất để làm Hour Bucket
                    LocalDateTime hourBucket = LocalDateTime.ofInstant(Instant.ofEpochMilli(eventTsMs), ZoneId.systemDefault())
                            .truncatedTo(ChronoUnit.HOURS);

                    EpisodeHourKey logKey = new EpisodeHourKey(episodeId, hourBucket);
                    logDeltaMap.put(logKey, logDeltaMap.getOrDefault(logKey, 0) + delta);
                }
            }

            // === 3. SAU KHI ĐÃ GOM CỤM XONG TRÊN RAM -> ĐẨY XUỐNG DB MỘT LẦN DUY NHẤT ===

            // Cập nhật các bảng thực thể chính (Mục 3 & 5)
            episodeDeltaMap.forEach((episodeId, totalDelta) -> {
                if (totalDelta != 0) {
                    String seriesId = episodeService.getSeriesIdByEpisodeId(episodeId);
                    aggregationRepository.updateEpisodeCommentCount(episodeId, totalDelta);
                    aggregationRepository.updateSeriesCommentCount(seriesId, totalDelta);
                    aggregationRepository.updateCampaignSeriesCommentCount(seriesId, totalDelta);
                    aggregationRepository.updateCampaignCommentCountAndTarget(seriesId, totalDelta);
                    aggregationRepository.updateCreatorCommentCount(seriesId, totalDelta);
                }
            });

            // Cập nhật các bảng Log theo Hour Bucket (Mục 4) bằng Upsert Native SQL
            logDeltaMap.forEach((key, totalDelta) -> {
                if (totalDelta != 0) {
                    String seriesId = episodeService.getSeriesIdByEpisodeId(key.getEpisodeId());
                    aggregationRepository.upsertEpisodeLog(key.getEpisodeId(), key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCampaignSeriesLog(seriesId, key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCampaignLog(seriesId, key.getHourBucket(), totalDelta);
                    aggregationRepository.upsertCreatorLogComments(seriesId, key.getHourBucket(), totalDelta);
                }
            });

        } catch (Exception e) {
            log.error("Lỗi xử lý Batch Comment Worker: ", e);
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR, "Comment worker error: " + e.getMessage());
        }
    }

    private JsonNode getTargetNode(JsonNode rootNode) {
        JsonNode targetNode = rootNode.get("after");
        if (targetNode == null) {
            targetNode = rootNode.get("before");
        }

        return targetNode;
    }
}
