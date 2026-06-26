package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignResponseDto;

public interface ICampaignService {
    CampaignResponseDto createCampaign(CampaignRequestDto requestDto);

    BasePageResponse<CampaignResponseDto> filterCampaigns(String[] statuses, String[] types,
            java.util.Map<String, Object> criteria, String sortBy, String sortDirection, Integer page,
            Integer pageSize);

    CampaignResponseDto getCampaignById(String campaignId);

    CampaignResponseDto updateCampaign(String campaignId, CampaignRequestDto requestDto);

    void deleteCampaign(String campaignId);
}
