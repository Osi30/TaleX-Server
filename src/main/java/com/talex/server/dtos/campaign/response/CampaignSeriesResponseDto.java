package com.talex.server.dtos.campaign.response;

import com.talex.server.entities.analytic.AnalyticData;
import com.talex.server.enums.engagement.CampaignStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSeriesResponseDto {
    private String campaignSeriesId;
    private String campaignId;
    private String seriesId;
    private CampaignStatus status;
    private AnalyticData analyticData;
    private Long totalImpression;
}
