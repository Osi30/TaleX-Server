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
}
