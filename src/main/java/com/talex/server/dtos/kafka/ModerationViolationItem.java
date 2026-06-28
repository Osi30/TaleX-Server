package com.talex.server.dtos.kafka;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * A single content moderation violation segment returned by the Python moderation service.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ModerationViolationItem {
    private Float timestampMs;
    private Float endTimestampMs;
    private String label;
    private Float confidence;
    private String suggestion;
}
