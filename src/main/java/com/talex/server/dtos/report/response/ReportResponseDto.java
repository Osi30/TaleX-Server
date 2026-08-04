package com.talex.server.dtos.report.response;

import com.talex.server.enums.report.ReportReason;
import com.talex.server.enums.report.ReportStatus;
import com.talex.server.enums.report.TargetType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportResponseDto {
    private String reportId;
    private String reporterId;
    private TargetType targetType;
    private String targetId;
    private ReportReason reason;
    private String description;
    private String proofImages;
    private ReportStatus status;
    private String ticketId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}