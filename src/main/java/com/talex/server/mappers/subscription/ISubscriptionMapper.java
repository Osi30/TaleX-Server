package com.talex.server.mappers.subscription;

import com.talex.server.dtos.requests.subscription.SubscriptionRequestDto;
import com.talex.server.dtos.responses.subscription.SubscriptionResponseDto;
import com.talex.server.entities.subscription.Subscription;

public interface ISubscriptionMapper {
    Subscription toEntity(SubscriptionRequestDto requestDto);

    SubscriptionResponseDto toResponseDto(Subscription subscription);

    void updateEntity(SubscriptionRequestDto requestDto, Subscription subscription);
}
