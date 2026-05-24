package com.talex.server.dtos.requests;

import com.talex.server.enums.StepType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class KycStepRequestDto {
    private StepType stepType;
    private Boolean isSuccess;
    private String message;
    private String provider;
    private String rawResponse;
    private String sessionId;
}
