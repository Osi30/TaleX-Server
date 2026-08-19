package com.talex.server.dtos.subscription.dtos;

public record SubscriptionGroupKey(
        Double subscriptionFee,
        Long durationDays
) {}