package com.talex.server.services.media;

import com.talex.server.dtos.kafka.CopyrightResultMessage;
import com.talex.server.dtos.kafka.ModerationResultMessage;
import com.talex.server.entities.media.Media;

/**
 * Orchestrates the content pipeline state machine:
 * PENDING -> copyright check -> moderation check -> ACTIVE/INACTIVE
 */
public interface ContentPipelineService {

    /** Dispatches a Kafka job to start copyright fingerprinting for the given media. */
    void dispatchPipelineJob(Media media);

    /** Handles the copyright result from Python and triggers moderation or blocks the media. */
    void handleCopyrightResult(CopyrightResultMessage result);

    /** Handles the moderation result from Python and sets the final media status. */
    void handleModerationResult(ModerationResultMessage result);

    /** Best-effort notify để AI service dọn fingerprint Milvus tương ứng khi media bị xóa. */
    void notifyMediaDeleted(String mediaId);

    /**
     * Đánh dấu FAILED khi BE xử lý kết quả (đã hợp lệ từ AI) bị lỗi ngoài dự kiến (DB tạm
     * gián đoạn, bug...) — không có bước này, exception bị nuốt trong
     * ContentPipelineWorker khiến message coi như "đã xử lý" (offset vẫn commit), media
     * kẹt vĩnh viễn ở trạng thái trung gian, không redeliver, không có nút "Thử lại".
     */
    void markProcessingFailed(String mediaId, String failedStep, String errorMessage);
}
