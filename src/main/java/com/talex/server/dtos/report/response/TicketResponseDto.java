package com.talex.server.dtos.report.response;

import com.talex.server.enums.report.TargetType;
import com.talex.server.enums.report.TicketStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TicketResponseDto {
    private String ticketId;
    private TargetType targetType;
    private String targetId;
    private Integer reportCount;
    private Integer priorityScore;
    private TicketStatus status;
    private String assignedStaffId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<ReportResponseDto> reports;
}