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

    @KafkaListener(topics = "content-copyright-result", groupId = "content-pipeline-copyright-group1")
    public void consumeCopyrightResult(String message) {
        try {
            CopyrightResultMessage result = objectMapper.readValue(message, CopyrightResultMessage.class);
            log.info("Copyright result received: mediaId={}, duplicate={}", result.getMediaId(), result.getIsDuplicate());
            pipelineService.handleCopyrightResult(result);
        } catch (Exception e) {
            log.error("Failed to process copyright result: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "content-moderation-result", groupId = "content-pipeline-moderation-group1")
    public void consumeModerationResult(String message) {
        try {
            ModerationResultMessage result = objectMapper.readValue(message, ModerationResultMessage.class);
            log.info("Moderation result received: mediaId={}, safe={}", result.getMediaId(), result.getIsSafe());
            pipelineService.handleModerationResult(result);
        } catch (Exception e) {
            log.error("Failed to process moderation result: {}", e.getMessage(), e);
        }
    }
}
