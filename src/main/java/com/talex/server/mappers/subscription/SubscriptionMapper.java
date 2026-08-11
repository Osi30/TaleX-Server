package com.talex.server.mappers.subscription;

import com.talex.server.dtos.subscription.request.SubscriptionRequestDto;
import com.talex.server.dtos.subscription.response.SubscriptionResponseDto;
import com.talex.server.entities.subscription.Subscription;

public interface SubscriptionMapper {
    Subscription toEntity(SubscriptionRequestDto requestDto);

    SubscriptionResponseDto toResponseDto(Subscription subscription);

    void updateEntity(SubscriptionRequestDto requestDto, Subscription subscription);
}
