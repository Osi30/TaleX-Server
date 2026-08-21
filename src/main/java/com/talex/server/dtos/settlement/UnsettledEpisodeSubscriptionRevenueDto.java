package com.talex.server.dtos.settlement;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UnsettledEpisodeSubscriptionRevenueDto {
    private String episodeId;
    private BigDecimal unsettledAmount;
}