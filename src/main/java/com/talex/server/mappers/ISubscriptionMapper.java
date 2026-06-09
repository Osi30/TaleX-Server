package com.talex.server.mappers;

import com.talex.server.dtos.requests.SubscriptionRequestDto;
import com.talex.server.dtos.responses.SubscriptionResponseDto;
import com.talex.server.entities.Subscription;

public interface ISubscriptionMapper {
    Subscription toEntity(SubscriptionRequestDto requestDto);

    SubscriptionResponseDto toResponseDto(Subscription subscription);

    void updateEntity(SubscriptionRequestDto requestDto, Subscription subscription);
}
