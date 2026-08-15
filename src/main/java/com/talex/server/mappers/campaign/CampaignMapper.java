package com.talex.server.mappers.campaign;

import com.talex.server.dtos.campaign.response.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;

public interface CampaignMapper {
    CampaignResponseDto toResponseDto(Campaign entity);
}
