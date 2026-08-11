package com.talex.server.services.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.subscription.request.SubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.SubscriptionFilterRequestDto;
import com.talex.server.dtos.subscription.response.CreatorPoolSummaryResponseDto;
import com.talex.server.dtos.subscription.response.SubscriptionResponseDto;
import com.talex.server.entities.subscription.Subscription;

public interface SubscriptionService {
    SubscriptionResponseDto createSubscription(SubscriptionRequestDto requestDto);

    BasePageResponse<SubscriptionResponseDto> filterSubscriptions(SubscriptionFilterRequestDto filterRequest);

    SubscriptionResponseDto getSubscriptionById(String subscriptionId);

    SubscriptionResponseDto updateSubscription(String subscriptionId, SubscriptionRequestDto requestDto);

    void deleteSubscription(String subscriptionId);

    Subscription getSubscriptionByIdEntity(String subscriptionId);

    CreatorPoolSummaryResponseDto getCreatorPoolSummary(int year, int month, String subscriptionId);
}
