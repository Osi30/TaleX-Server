package com.talex.server.services.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.kafka.PipelineJobMessage;
import com.talex.server.exceptions.codes.ContentPipelineErrorCode;
import com.talex.server.exceptions.details.ContentPipelineException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

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

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public void sendPipelineJob(PipelineJobMessage message) {
        sendMessage(TOPIC_PIPELINE_JOB, message.getMediaId(), message);
    }

    public void sendModerationJob(PipelineJobMessage message) {
        sendMessage(TOPIC_MODERATION_JOB, message.getMediaId(), message);
    }

    private void sendMessage(String topic, String key, Object message) {
        try {
            String json = objectMapper.writeValueAsString(message);
            kafkaTemplate.send(topic, key, json);
            log.info("Kafka message sent: topic={}, key={}", topic, key);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize Kafka message: topic={}, key={}", topic, key, e);
            throw new ContentPipelineException(
                    ContentPipelineErrorCode.KAFKA_SEND_FAILED,
                    "Failed to dispatch pipeline job: " + e.getMessage());
        }
    }
}
