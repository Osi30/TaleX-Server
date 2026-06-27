package com.talex.server.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Incoming message from Python moderation service via Kafka topic content-moderation-result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModerationResultMessage {
    private String mediaId;
    private String correlationId;
    private Boolean isSafe;
    private String primaryLabel;
    private Float confidenceScore;
    private List<ModerationViolationItem> violations;
    private String rawResponse;
    private String processedAt;     // ISO-8601 string
    private Boolean success;
    private String errorMessage;
}
