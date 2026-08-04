package com.talex.server.services.report;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.ReportRequestDto;
import com.talex.server.dtos.report.response.ReportResponseDto;

public interface ReportService {
    ReportResponseDto createReport(String currentUserId, String role, ReportRequestDto requestDto);
    BasePageResponse<ReportResponseDto> getUserReports(String currentUserId, BaseFilterRequestDto filterRequest);
}
