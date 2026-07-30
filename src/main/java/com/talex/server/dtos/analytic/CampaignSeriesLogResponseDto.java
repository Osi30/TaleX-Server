package com.talex.server.dtos.analytic;

import com.talex.server.entities.analytic.AnalyticData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSeriesLogResponseDto {
    private String campaignSeriesLogId;
    private String campaignSeriesId;
    private LocalDateTime hourBucket;
    private AnalyticData analyticData;
    private Long totalImpression;
}