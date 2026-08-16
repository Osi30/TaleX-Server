package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.campaign.request.EngagementServiceRequestDto;
import com.talex.server.dtos.requests.filters.EngagementServiceFilterRequestDto;
import com.talex.server.dtos.campaign.response.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;

public interface EngagementServiceService {
    EngagementServiceResponseDto createEngagementService(EngagementServiceRequestDto requestDto);

    BasePageResponse<EngagementServiceResponseDto> filterEngagementServices(
            EngagementServiceFilterRequestDto filterRequest);

    EngagementServiceResponseDto getEngagementServiceById(String engagementServiceId);

    EngagementService findById(String id);

    EngagementService findActive(String id);

    EngagementServiceResponseDto updateEngagementService(String engagementServiceId,
                                                         EngagementServiceRequestDto requestDto);

    void deleteEngagementService(String engagementServiceId);
}
