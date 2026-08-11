package com.talex.server.dtos.statistics;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatisticTrendDto {
    private String period;
    private BigDecimal gmv;
    private BigDecimal netRevenue;
    private BigDecimal vatAmount;
    private Long totalCoin;
}