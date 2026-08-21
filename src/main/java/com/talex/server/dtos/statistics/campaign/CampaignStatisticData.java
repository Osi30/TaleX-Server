package com.talex.server.dtos.statistics.campaign;

import java.math.BigDecimal;

public record CampaignStatisticData(
        String period,
        BigDecimal grossRevenue,
        BigDecimal vatAmount,
        BigDecimal netRevenue
) {}