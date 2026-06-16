package com.talex.server.dtos.responses;

import com.talex.server.enums.kyc.KycStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class KycSessionResponseDto {
    private String kycSessionId;
    private KycStatus status;
    private LocalDateTime startedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime completedAt;
}
