package com.talex.server.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single copyright violation segment returned by the Python copyright check service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class CopyrightViolationItem {
    private String sourceMediaId;
    private Float startTimeTarget;
    private Float endTimeTarget;
    private Float startTimeSource;
    private Float endTimeSource;
    private Float similarityScore;
    private String violationType;   // "VIDEO", "IMAGE", "AUDIO"
}
