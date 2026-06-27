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
}
