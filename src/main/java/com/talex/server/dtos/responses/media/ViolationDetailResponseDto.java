package com.talex.server.dtos.responses.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDetailResponseDto {
    private String violationDetailId;
    private Float violationAt;
    private Float endViolationAt;
    private String label;
    private Float confidence;
    private String suggestion;
}
