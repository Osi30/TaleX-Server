package com.talex.server.records;

import java.math.BigDecimal;

public record OrderStatisticData(
        String period,
        BigDecimal gmv,
        BigDecimal netRevenue,
        BigDecimal vatAmount,
        BigDecimal totalCoin
) {
}
