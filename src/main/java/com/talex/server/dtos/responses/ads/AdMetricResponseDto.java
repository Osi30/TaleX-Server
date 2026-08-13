package com.talex.server.dtos.responses.ads;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdMetricResponseDto {
    private LocalDate reportDate;
    private Long impressions;
    private Long clicks;
    private Long focusedViews6s;
    private Long paidFocusedViews6s;
    private Long spend;
}
