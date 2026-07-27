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

    @KafkaListener(topics = "content-copyright-result", groupId = "content-pipeline-copyright-group9999")
    public void consumeCopyrightResult(String message) {
        // enable-auto-commit=false, ack-mode mặc định BATCH → offset commit ngay khi hàm
        // listener return bình thường, KỂ CẢ khi exception bị catch/nuốt bên trong. Nếu chỉ
        // log mà không đánh dấu FAILED, message coi như "đã xử lý" vĩnh viễn dù thực ra
        // chưa hề — media kẹt trạng thái trung gian, không redeliver, Creator không thấy
        // nút "Thử lại" vì status không đổi.
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

    @KafkaListener(topics = "content-moderation-result", groupId = "content-pipeline-moderation-group9999")
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
