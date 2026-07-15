package com.talex.server.records;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

public record MonetizationData(
        UUID accountId,
        BigDecimal totalSpentAmount,
        Long premiumSubscriptionCount,
        Long singlePurchaseCount,
        Long interactionPushCount,
        LocalDateTime lastPurchaseTime
) {
}
