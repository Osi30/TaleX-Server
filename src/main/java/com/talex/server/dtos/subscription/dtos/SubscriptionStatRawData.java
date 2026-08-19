package com.talex.server.dtos.subscription.dtos;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;

public record SubscriptionStatRawData(
        Object accountId,
        String accountSubscriptionId,
        LocalDateTime startTime,
        LocalDateTime endTime,
        BigDecimal totalAmount,
        BigDecimal vatAmount,
        String creatorId,
        String episodeId,
        Long views
) {
    public String getUserId() {
        return accountId != null ? accountId.toString() : "unknown_user";
    }

    public Double getNetAmount() {
        if (totalAmount == null) return 0.0;
        BigDecimal net = vatAmount != null ? totalAmount.subtract(vatAmount) : totalAmount;
        return net.doubleValue();
    }

    public Long getDurationDays() {
        if (startTime == null || endTime == null) return 0L;
        return Duration.between(startTime, endTime).toDays();
    }

    public SubscriptionGroupKey toGroupKey() {
        return new SubscriptionGroupKey(getNetAmount(), getDurationDays());
    }
}