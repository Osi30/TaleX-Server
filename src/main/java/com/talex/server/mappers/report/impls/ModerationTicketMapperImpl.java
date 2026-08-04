package com.talex.server.mappers.report.impls;

import com.talex.server.dtos.report.response.TicketResponseDto;
import com.talex.server.entities.report.ModerationTicket;
import com.talex.server.mappers.report.IModerationTicketMapper;
import com.talex.server.mappers.report.IReportMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ModerationTicketMapperImpl implements IModerationTicketMapper {
    private final IReportMapper reportMapper;

    @Override
    public TicketResponseDto toResponseDto(ModerationTicket entity) {
        if (entity == null) return null;
        return TicketResponseDto.builder()
                .ticketId(entity.getTicketId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .reportCount(entity.getReportCount())
                .priorityScore(entity.getPriorityScore())
                .status(entity.getStatus())
                .assignedStaffId(entity.getAssignedStaffId())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .reports(entity.getReports() != null ?
                        entity.getReports().stream().map(reportMapper::toResponseDto).toList() : null)
                .build();
    }
}