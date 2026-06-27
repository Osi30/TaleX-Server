package com.talex.server.services.campaign;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.campaign.CampaignLogRequestDto;
import com.talex.server.dtos.responses.campaign.CampaignLogResponseDto;

public interface ICampaignLogService {
    CampaignLogResponseDto createCampaignLog(CampaignLogRequestDto requestDto);

    BasePageResponse<CampaignLogResponseDto> filterCampaignLogs(String[] eventTypes,
            java.util.Map<String, Object> criteria, String sortBy, String sortDirection, Integer page,
            Integer pageSize);

    CampaignLogResponseDto getCampaignLogById(String campaignLogId);

    void deleteCampaignLog(String campaignLogId);
}
