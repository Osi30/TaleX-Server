package com.talex.server.mappers.campaign;

import com.talex.server.dtos.responses.campaign.CampaignResponseDto;
import com.talex.server.entities.campaign.Campaign;

public interface ICampaignMapper {
    CampaignResponseDto toResponseDto(Campaign entity);
}
