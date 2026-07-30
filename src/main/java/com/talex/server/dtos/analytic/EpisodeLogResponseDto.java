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
public class EpisodeLogResponseDto {
    private String episodeLogId;
    private LocalDateTime hourBucket;
    private AnalyticData analyticData;
}