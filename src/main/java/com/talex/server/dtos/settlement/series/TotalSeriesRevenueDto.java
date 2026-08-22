package com.talex.server.dtos.settlement.series;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TotalSeriesRevenueDto {
    private String seriesId;
    private BigDecimal unsettledDirectAmount;
    private BigDecimal unsettledSubscriptionAmount;
    private BigDecimal totalUnsettledAmount;
}