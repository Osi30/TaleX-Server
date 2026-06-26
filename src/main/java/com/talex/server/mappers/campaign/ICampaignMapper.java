package com.talex.server.mappers.campaign;

import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;

public interface ICampaignMapper {
    Campaign toEntity(CampaignRequestDto requestDto);

    CampaignResponseDto toResponseDto(Campaign entity);

    void updateEntity(CampaignRequestDto requestDto, Campaign entity);
}
