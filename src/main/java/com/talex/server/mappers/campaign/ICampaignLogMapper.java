package com.talex.server.mappers.campaign;

import com.talex.server.dtos.requests.campaign.CampaignLogRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignLogResponseDto;
import com.talex.server.entities.campaign.CampaignLog;

public interface ICampaignLogMapper {
    CampaignLog toEntity(CampaignLogRequestDto requestDto);

    CampaignLogResponseDto toResponseDto(CampaignLog entity);
}
