package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.campaign.request.CampaignRequestDto;
import com.talex.server.dtos.campaign.request.CampaignUpdateDto;
import com.talex.server.dtos.requests.filters.CampaignFilterRequestDto;
import com.talex.server.dtos.campaign.response.CampaignResponseDto;

import java.util.List;
import java.util.UUID;

public interface CampaignService {
    CampaignResponseDto createCampaign(CampaignRequestDto requestDto);

    void validateCampaign(UUID accountId, List<String> seriesIds);

    BasePageResponse<CampaignResponseDto> filterCampaigns(CampaignFilterRequestDto filterRequest);

    CampaignResponseDto getCampaignById(String campaignId);

    CampaignResponseDto updateCampaign(String campaignId, CampaignUpdateDto requestDto);

    void deleteCampaign(String campaignId);
}
