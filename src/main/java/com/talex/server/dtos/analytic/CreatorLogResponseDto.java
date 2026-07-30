package com.talex.server.dtos.analytic;

import com.talex.server.entities.analytic.AnalyticData;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorLogResponseDto {
    private String creatorLogId;
    private LocalDateTime hourBucket;
    private AnalyticData analyticData;
    private Long follows;
}