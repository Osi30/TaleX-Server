package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.requests.campaign.CampaignUpdateDto;
import com.talex.server.dtos.requests.filters.CampaignFilterRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;

public interface ICampaignService {
    CampaignResponseDto createCampaign(CampaignRequestDto requestDto);

    BasePageResponse<CampaignResponseDto> filterCampaigns(CampaignFilterRequestDto filterRequest);

    CampaignResponseDto getCampaignById(String campaignId);

    CampaignResponseDto updateCampaign(String campaignId, CampaignUpdateDto requestDto);

    void deleteCampaign(String campaignId);
}
