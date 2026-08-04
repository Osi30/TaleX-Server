package com.talex.server.services.media.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.kafka.PipelineJobMessage;
import com.talex.server.exceptions.codes.ContentPipelineErrorCode;
import com.talex.server.exceptions.details.ContentPipelineException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka producer for content pipeline jobs.
 * Sends JSON-serialized messages to Python AI service topics.
 */
@Component
@Slf4j
public class ContentPipelineProducer {

    private final String topicPipelineJob;
    private final String topicModerationJob;
    private final String topicMediaDelete;

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    // KAFKA_TOPIC_SUFFIX cho phép local dev dùng topic riêng (vd "-local"), tách biệt hoàn
    // toàn khỏi VPS dùng chung 1 cụm Kafka Aiven — mặc định rỗng nên VPS không cần đổi gì.
    public ContentPipelineProducer(KafkaTemplate<String, String> kafkaTemplate,
                                    ObjectMapper objectMapper,
                                    @Value("${KAFKA_TOPIC_SUFFIX:}") String topicSuffix) {
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
        this.topicPipelineJob = "content-pipeline-job" + topicSuffix;
        this.topicModerationJob = "content-moderation-job" + topicSuffix;
        this.topicMediaDelete = "content-media-delete" + topicSuffix;
    }

    public void sendPipelineJob(PipelineJobMessage message) {
        sendMessage(topicPipelineJob, message.getMediaId(), message);
    }

    public void sendModerationJob(PipelineJobMessage message) {
        sendMessage(topicModerationJob, message.getMediaId(), message);
    }

    public void sendMediaDeleted(String mediaId) {
        sendMessage(topicMediaDelete, mediaId, Map.of("mediaId", mediaId));
    }

    private void sendMessage(String topic, String key, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            // kafkaTemplate.send() trả về CompletableFuture — trước đây bị bỏ qua, log
            // "sent" ngay sau khi GỌI hàm gửi chứ không phải sau khi broker THẬT SỰ nhận.
            // Nếu gửi thất bại không đồng bộ (mất kết nối Aiven tạm thời...), không có gì
            // phát hiện được — job coi như đã dispatch nhưng không bao giờ tới AI, media
            // kẹt PENDING vĩnh viễn. Gắn callback để log rõ ràng khi gửi thật sự thất bại.
            kafkaTemplate.send(topic, key, json).whenComplete((result, ex) -> {
                if (ex != null) {
                    log.error("Kafka send FAILED: topic={}, key={}", topic, key, ex);
                } else {
                    log.info("Kafka message sent: topic={}, key={}", topic, key);
                }
            });
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Kafka message: topic={}, key={}", topic, key, e);
            throw new ContentPipelineException(
                    ContentPipelineErrorCode.KAFKA_SEND_FAILED,
                    "Failed to dispatch pipeline job: " + e.getMessage());
        }
    }
}
