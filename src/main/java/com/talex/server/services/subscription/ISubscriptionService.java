package com.talex.server.services.subscription;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.subscription.SubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.SubscriptionFilterRequestDto;
import com.talex.server.dtos.responses.subscription.SubscriptionResponseDto;
import com.talex.server.entities.subscription.Subscription;

public interface ISubscriptionService {
    SubscriptionResponseDto createSubscription(SubscriptionRequestDto requestDto);

    BasePageResponse<SubscriptionResponseDto> filterSubscriptions(SubscriptionFilterRequestDto filterRequest);

    SubscriptionResponseDto getSubscriptionById(String subscriptionId);

    SubscriptionResponseDto updateSubscription(String subscriptionId, SubscriptionRequestDto requestDto);

    void deleteSubscription(String subscriptionId);

    Subscription getSubscriptionByIdEntity(String subscriptionId);
}
