package com.talex.server.services.report;

import com.talex.server.dtos.BaseFilterRequestDto;
import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.report.response.PenaltyResponseDto;

public interface PenaltyService {
    BasePageResponse<PenaltyResponseDto> getMyPenalties(String currentUserId, BaseFilterRequestDto filterRequest);
    BasePageResponse<PenaltyResponseDto> filterPenalties(BaseFilterRequestDto filterRequest);
    PenaltyResponseDto getPenaltyById(String penaltyId);
    PenaltyResponseDto revokePenalty(String penaltyId, String adminId, String role, String reason);
}
