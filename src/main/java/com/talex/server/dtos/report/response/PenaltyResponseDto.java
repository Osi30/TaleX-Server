package com.talex.server.dtos.report.response;

import com.talex.server.enums.report.PenaltyLevel;
import com.talex.server.enums.report.PenaltyStatus;
import com.talex.server.enums.report.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PenaltyResponseDto {
    private String penaltyId;
    private String ticketId;
    private String targetUserId;
    private String issuerId;
    private PenaltyLevel level;
    private TargetType targetType;
    private String targetId;
    private String reason;
    private PenaltyStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}