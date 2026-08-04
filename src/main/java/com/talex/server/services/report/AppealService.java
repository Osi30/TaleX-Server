package com.talex.server.services.report;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.request.AppealProcessRequestDto;
import com.talex.server.dtos.report.request.AppealRequestDto;
import com.talex.server.dtos.report.response.AppealResponseDto;

public interface AppealService {
    AppealResponseDto createAppeal(String currentUserId, String role, String penaltyId, AppealRequestDto requestDto);
    AppealResponseDto processAppeal(String adminId, String role, String appealId, AppealProcessRequestDto requestDto);
    BasePageResponse<AppealResponseDto> filterAppeals(BaseFilterRequestDto filterRequest);
    BasePageResponse<AppealResponseDto> filterAppeals(String accountId, BaseFilterRequestDto filterRequest);
    AppealResponseDto getAppealByPenaltyId(String penaltyId);
}
