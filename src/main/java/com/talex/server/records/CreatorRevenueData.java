package com.talex.server.records;

public record CreatorRevenueData(
        String creatorId,
        Double totalRevenue,
        String subscriptionResultId
) {
}
