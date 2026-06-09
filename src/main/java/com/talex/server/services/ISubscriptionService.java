package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.SubscriptionRequestDto;
import com.talex.server.dtos.requests.filters.SubscriptionFilterRequestDto;
import com.talex.server.dtos.responses.SubscriptionResponseDto;
import com.talex.server.entities.Subscription;

public interface ISubscriptionService {
    SubscriptionResponseDto createSubscription(SubscriptionRequestDto requestDto);

    BasePageResponse<SubscriptionResponseDto> filterSubscriptions(SubscriptionFilterRequestDto filterRequest);

    SubscriptionResponseDto getSubscriptionById(String subscriptionId);

    SubscriptionResponseDto updateSubscription(String subscriptionId, SubscriptionRequestDto requestDto);

    void deleteSubscription(String subscriptionId);

    Subscription getSubscriptionByIdEntity(String subscriptionId);
}
