package com.talex.server.mappers.campaign;

import com.talex.server.dtos.requests.campaign.EngagementServiceRequestDto;
import com.talex.server.dtos.responses.campaign.EngagementServiceResponseDto;
import com.talex.server.entities.campaign.EngagementService;

public interface IEngagementServiceMapper {
    EngagementService toEntity(EngagementServiceRequestDto requestDto);

    EngagementServiceResponseDto toResponseDto(EngagementService entity);

    void updateEntity(EngagementServiceRequestDto requestDto, EngagementService entity);
}
