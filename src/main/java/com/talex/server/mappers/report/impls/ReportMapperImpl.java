package com.talex.server.mappers.report.impls;

import com.talex.server.dtos.report.request.ReportRequestDto;
import com.talex.server.dtos.report.response.ReportResponseDto;
import com.talex.server.entities.report.Report;
import com.talex.server.mappers.report.IReportMapper;
import org.springframework.stereotype.Component;

@Component
public class ReportMapperImpl implements IReportMapper {

    @Override
    public Report toEntity(ReportRequestDto requestDto) {
        if (requestDto == null) return null;
        return Report.builder()
                .targetType(requestDto.getTargetType())
                .targetId(requestDto.getTargetId())
                .reason(requestDto.getReason())
                .description(requestDto.getDescription())
                .proofImages(requestDto.getProofImages())
                .build();
    }

    @Override
    public ReportResponseDto toResponseDto(Report entity) {
        if (entity == null) return null;
        return ReportResponseDto.builder()
                .reportId(entity.getReportId())
                .reporterId(entity.getReporterId())
                .targetType(entity.getTargetType())
                .targetId(entity.getTargetId())
                .reason(entity.getReason())
                .description(entity.getDescription())
                .proofImages(entity.getProofImages())
                .status(entity.getStatus())
                .ticketId(entity.getModerationTicket() != null ? entity.getModerationTicket().getTicketId() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}