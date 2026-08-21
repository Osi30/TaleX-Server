package com.talex.server.dtos.statistics.subscription;

import java.math.BigDecimal;

public record SubscriptionStatisticData(
        String period,
        BigDecimal grossRevenue,
        BigDecimal vatAmount,
        BigDecimal netRevenue
) {}