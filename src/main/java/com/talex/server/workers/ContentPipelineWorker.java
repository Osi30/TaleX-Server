package com.talex.server.workers;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.kafka.CopyrightResultMessage;
import com.talex.server.dtos.kafka.ModerationResultMessage;
import com.talex.server.services.media.ContentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

/**
 * Kafka consumers for content pipeline result topics.
 * Receives copyright and moderation results from Python AI service.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ContentPipelineWorker {

    private final ContentPipelineService pipelineService;
    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "content-copyright-result${KAFKA_TOPIC_SUFFIX:}",
            groupId = "content-pipeline-copyright${KAFKA_TOPIC_SUFFIX:}")
    public void consumeCopyrightResult(String message) {
        String mediaId = null;
        try {
            CopyrightResultMessage result = objectMapper.readValue(message, CopyrightResultMessage.class);
            mediaId = result.getMediaId();
            log.info("Copyright result received: mediaId={}, duplicate={}", result.getMediaId(), result.getIsDuplicate());
            pipelineService.handleCopyrightResult(result);
        } catch (Exception e) {
            log.error("Failed to process copyright result: {}", e.getMessage(), e);
            if (mediaId != null) {
                pipelineService.markProcessingFailed(mediaId, "COPYRIGHT", "Lỗi xử lý kết quả bản quyền: " + e.getMessage());
            }
        }
    }

    @KafkaListener(topics = "content-moderation-result${KAFKA_TOPIC_SUFFIX:}",
            groupId = "content-pipeline-moderation-group${KAFKA_TOPIC_SUFFIX:}")
    public void consumeModerationResult(String message) {
        String mediaId = null;
        try {
            ModerationResultMessage result = objectMapper.readValue(message, ModerationResultMessage.class);
            mediaId = result.getMediaId();
            log.info("Moderation result received: mediaId={}, safe={}", result.getMediaId(), result.getIsSafe());
            pipelineService.handleModerationResult(result);
        } catch (Exception e) {
            log.error("Failed to process moderation result: {}", e.getMessage(), e);
            if (mediaId != null) {
                pipelineService.markProcessingFailed(mediaId, "MODERATION", "Lỗi xử lý kết quả kiểm duyệt: " + e.getMessage());
            }
        }
    }
}
