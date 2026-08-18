package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

/**
 * Request body for the irreversible admin media purge. A justification is mandatory — this action
 * hard-deletes content and is persisted to a legal audit log (media_purge_log).
 */
@Data
public class MediaPurgeRequestDto {

    @NotBlank(message = "Purge reason is required")
    private String reason;
}
