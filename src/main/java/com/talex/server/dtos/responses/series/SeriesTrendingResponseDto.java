package com.talex.server.dtos.responses.series;

import com.talex.server.entities.analytic.AnalyticData;
import com.talex.server.entities.analytic.TrendingAnalyticData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesTrendingResponseDto {
    private String seriesId;
    private String title;
    private String coverUrl;
    private String bannerUrl;
    private TrendingAnalyticData trendingAnalyticData = new TrendingAnalyticData();
    private AnalyticData analyticData = new AnalyticData();
    private Double totalRating = 0D;
    private Long ratingCount = 0L;
    private Double averageRating = 0.0;

    private Double wilsonScore;
    private Double upperWilsonScore;
    private Double rankingScore;
}
