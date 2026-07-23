package com.talex.server.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * Incoming message from Python copyright check service via Kafka topic content-copyright-result.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopyrightResultMessage {
    private String mediaId;
    private String correlationId;
    private String contentId;
    private Boolean isDuplicate;
    private Float overallSimilarity;
    private Integer fingerprintCount;
    private List<CopyrightViolationItem> violations;
    private String processedAt;     // ISO-8601 string
    private Boolean success;
    private String errorMessage;
    private String previewS3Key;
}
