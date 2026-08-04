package com.talex.server.mappers.report;


import com.talex.server.dtos.report.request.ReportRequestDto;
import com.talex.server.dtos.report.response.ReportResponseDto;
import com.talex.server.entities.report.Report;

public interface IReportMapper {
    Report toEntity(ReportRequestDto requestDto);

    ReportResponseDto toResponseDto(Report entity);
}