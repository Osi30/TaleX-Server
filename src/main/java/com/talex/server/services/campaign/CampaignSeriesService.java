package com.talex.server.services.campaign;

import com.talex.server.dtos.responses.campaign.CampaignSeriesResponseDto;
import com.talex.server.enums.engagement.CampaignStatus;

import java.util.List;

public interface CampaignSeriesService {
    List<CampaignSeriesResponseDto> getByCampaignId(String campaignId);

    CampaignSeriesResponseDto updateStatus(String campaignSeriesId, CampaignStatus newStatus);

    CampaignSeriesResponseDto cancelCampaignSeries(String campaignSeriesId);
}
