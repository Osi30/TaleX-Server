package com.talex.server.services.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.subscription.response.SubscriptionResultResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionRevenueLogDetailResponseDto;

public interface SubscriptionResultService {
    SubscriptionResultResponseDto getSubscriptionResultByMonthYear(int year, int month);

    BasePageResponse<SubscriptionRevenueLogDetailResponseDto> getRevenueLogsByResultId(
            String subscriptionResultId, int page, int pageSize);
}
