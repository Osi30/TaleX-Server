package com.talex.server.dtos.statistics.content;

import java.math.BigDecimal;

public record ContentRevenueStatisticData(
        String period,
        BigDecimal grossRevenue,
        BigDecimal vatAmount,
        BigDecimal coinAmount,
        BigDecimal creatorShareAmount,
        BigDecimal netRevenue
) {}