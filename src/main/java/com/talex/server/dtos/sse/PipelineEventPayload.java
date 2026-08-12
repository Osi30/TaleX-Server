package com.talex.server.dtos.sse;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * SSE event payload for pipeline status notifications.
 * Sent to creators when copyright check, moderation check, or failure occurs.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PipelineEventPayload {
    private String mediaId;
    private String status;
    private String contentId;
    private Boolean isDuplicate;
    private Integer violationsCount;
    private Boolean isSafe;
    private String primaryLabel;
    private String errorMessage;
    private String failedStep;
    // "APPROVED" | "REJECTED" | "PENDING_REVIEW" — FE cần phân biệt để không báo nhầm
    // "bị từ chối" khi thực ra đang chờ Staff duyệt (case series MATURE).
    private String approvalStatus;
    // Ghi chú của Staff khi từ chối thủ công (khác primaryLabel — đó là nhãn AI tự động
    // phát hiện, cái này là lý do người thật gõ tay). Chỉ có giá trị ở event
    // "pipeline:staff_rejected".
    private String reviewerNotes;
}
