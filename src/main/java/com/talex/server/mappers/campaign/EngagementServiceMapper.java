package com.talex.server.mappers.campaign;

import com.talex.server.dtos.campaign.request.EngagementServiceRequestDto;
import com.talex.server.dtos.campaign.response.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;

public interface EngagementServiceMapper {
    EngagementService toEntity(EngagementServiceRequestDto requestDto);

    EngagementServiceResponseDto toResponseDto(EngagementService entity);

    void updateEntity(EngagementServiceRequestDto requestDto, EngagementService entity);
}
