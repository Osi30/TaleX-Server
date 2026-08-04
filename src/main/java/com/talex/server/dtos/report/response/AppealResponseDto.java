package com.talex.server.dtos.report.response;

import com.talex.server.enums.report.AppealStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AppealResponseDto {
    private String appealId;
    private String penaltyId;
    private String appellantId;
    private String reviewerId;
    private String reason;
    private String proofDocuments;
    private AppealStatus status;
    private String adminNote;
    private LocalDateTime resolvedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private PenaltyResponseDto penalty;
}