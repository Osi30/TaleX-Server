package com.talex.server.dtos.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticResponseDto {
    private StatisticOverviewDto overview;
    private List<StatisticTrendDto> trends;
}