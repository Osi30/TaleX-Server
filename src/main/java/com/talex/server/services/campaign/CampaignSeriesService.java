package com.talex.server.services.campaign;

import com.talex.server.dtos.analytic.CampaignSeriesLogResponseDto;
import com.talex.server.dtos.campaign.response.CampaignSeriesResponseDto;
import com.talex.server.enums.engagement.CampaignStatus;

import java.time.LocalDateTime;
import java.util.List;

public interface CampaignSeriesService {
    List<CampaignSeriesResponseDto> getByCampaignId(String campaignId);

    CampaignSeriesResponseDto updateStatus(String campaignSeriesId, CampaignStatus newStatus);

    CampaignSeriesResponseDto cancelCampaignSeries(String campaignSeriesId);

    List<CampaignSeriesLogResponseDto> getLogs(String campaignSeriesId, LocalDateTime startTime, LocalDateTime endTime);
}
