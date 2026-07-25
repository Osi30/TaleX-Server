package com.talex.server.dtos.responses.ads;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.util.UUID;

@Data
@Builder
public class AdMetricResponseDto {
    private UUID metricId;
    private UUID campaignId;
    private LocalDate reportDate;
    private Long impressions;
    private Long clicks;
}
