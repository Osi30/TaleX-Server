package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.EngagementServiceRequestDto;
import com.talex.server.dtos.requests.filters.EngagementServiceFilterRequestDto;
import com.talex.server.dtos.responses.campaign.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;

public interface IEngagementServiceService {
    EngagementServiceResponseDto createEngagementService(EngagementServiceRequestDto requestDto);

    BasePageResponse<EngagementServiceResponseDto> filterEngagementServices(
            EngagementServiceFilterRequestDto filterRequest);

    EngagementServiceResponseDto getEngagementServiceById(String engagementServiceId);
    EngagementService findById(String id);

    EngagementServiceResponseDto updateEngagementService(String engagementServiceId,
            EngagementServiceRequestDto requestDto);

    void deleteEngagementService(String engagementServiceId);
}
