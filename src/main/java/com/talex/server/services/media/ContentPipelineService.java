package com.talex.server.services.media;

import com.talex.server.dtos.kafka.CopyrightResultMessage;
import com.talex.server.dtos.kafka.ModerationResultMessage;
import com.talex.server.entities.media.Media;

import java.time.LocalDateTime;

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

    /**
     * Đánh dấu FAILED cho media kẹt quá lâu (dùng bởi ContentPipelineReconciler) bằng UPDATE
     * có điều kiện nguyên tử — khác markProcessingFailed() ở chỗ đường này CÓ THỂ đua với 1
     * kết quả hợp lệ đang commit cùng lúc, nên phải atomic thay vì read-then-write.
     */
    void markStalePipelineFailed(String mediaId, LocalDateTime staleBefore, String errorMessage, String failedStep);

    /**
     * Báo real-time (SSE) cho creator khi Staff từ chối thủ công kèm lý do — khác các sự
     * kiện pipeline khác (copyright/moderation complete) vốn bắn ngay lúc AI xử lý xong lúc
     * upload, event này bắn muộn hơn (lúc Staff thật sự bấm Từ chối, có thể sau đó rất lâu),
     * nên cần đường riêng thay vì tái dùng handleModerationResult().
     */
    void notifyStaffRejected(Media media, String reason);
}
