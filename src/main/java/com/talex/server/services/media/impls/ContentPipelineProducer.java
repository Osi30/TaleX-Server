package com.talex.server.services.media.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.kafka.PipelineJobMessage;
import com.talex.server.exceptions.codes.ContentPipelineErrorCode;
import com.talex.server.exceptions.details.ContentPipelineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Kafka producer for content pipeline jobs.
 * Sends JSON-serialized messages to Python AI service topics.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentPipelineProducer {

    private static final String TOPIC_PIPELINE_JOB = "content-pipeline-job";
    private static final String TOPIC_MODERATION_JOB = "content-moderation-job";
    private static final String TOPIC_MEDIA_DELETE = "content-media-delete";

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPipelineJob(PipelineJobMessage message) {
        sendMessage(TOPIC_PIPELINE_JOB, message.getMediaId(), message);
    }

    public void sendModerationJob(PipelineJobMessage message) {
        sendMessage(TOPIC_MODERATION_JOB, message.getMediaId(), message);
    }

    public void sendMediaDeleted(String mediaId) {
        sendMessage(TOPIC_MEDIA_DELETE, mediaId, Map.of("mediaId", mediaId));
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
