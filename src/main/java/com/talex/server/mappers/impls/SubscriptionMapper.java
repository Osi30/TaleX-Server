package com.talex.server.mappers.impls;

import com.talex.server.dtos.requests.SubscriptionRequestDto;
import com.talex.server.dtos.responses.SubscriptionResponseDto;
import com.talex.server.entities.Subscription;
import com.talex.server.mappers.ISubscriptionMapper;
import org.springframework.stereotype.Component;

import java.util.Optional;

@Component
public class SubscriptionMapper implements ISubscriptionMapper {

    @Override
    public Subscription toEntity(SubscriptionRequestDto requestDto) {
        if (requestDto == null) {
            return null;
        }

        return Subscription.builder()
                .tier(requestDto.getTier())
                .description(requestDto.getDescription())
                .price(requestDto.getPrice())
                .duration(requestDto.getDuration())
                .durationUnit(Optional.ofNullable(requestDto.getDurationUnit())
                        .map(Enum::toString)
                        .orElse(null))
                .isAdBlocked(Optional.ofNullable(requestDto.getIsAdBlocked()).orElse(false))
                .isMovieUnlocked(Optional.ofNullable(requestDto.getIsMovieUnlocked()).orElse(false))
                .isStoryUnlocked(Optional.ofNullable(requestDto.getIsStoryUnlocked()).orElse(false))
                .build();
    }

    @Override
    public SubscriptionResponseDto toResponseDto(Subscription subscription) {
        if (subscription == null) {
            return null;
        }

        return SubscriptionResponseDto.builder()
                .subscriptionId(subscription.getSubscriptionId())
                .tier(subscription.getTier())
                .description(subscription.getDescription())
                .price(subscription.getPrice())
                .duration(subscription.getDuration())
                .durationUnit(subscription.getDurationUnit())
                .totalPurchases(subscription.getTotalPurchases())
                .isDeleted(subscription.getIsDeleted())
                .isAdBlocked(subscription.getIsAdBlocked())
                .isMovieUnlocked(subscription.getIsMovieUnlocked())
                .isStoryUnlocked(subscription.getIsStoryUnlocked())
                .createdAt(subscription.getCreatedAt())
                .updatedAt(subscription.getUpdatedAt())
                .build();
    }

    @Override
    public void updateEntity(SubscriptionRequestDto requestDto, Subscription subscription) {
        if (requestDto == null || subscription == null) {
            return;
        }

        Optional.ofNullable(requestDto.getTier()).ifPresent(subscription::setTier);
        Optional.ofNullable(requestDto.getDescription()).ifPresent(subscription::setDescription);
        Optional.ofNullable(requestDto.getPrice()).ifPresent(subscription::setPrice);
        Optional.ofNullable(requestDto.getDuration()).ifPresent(subscription::setDuration);
        Optional.ofNullable(requestDto.getDurationUnit())
                .map(Enum::toString)
                .ifPresent(subscription::setDurationUnit);
        Optional.ofNullable(requestDto.getIsAdBlocked()).ifPresent(subscription::setIsAdBlocked);
        Optional.ofNullable(requestDto.getIsMovieUnlocked()).ifPresent(subscription::setIsMovieUnlocked);
        Optional.ofNullable(requestDto.getIsStoryUnlocked()).ifPresent(subscription::setIsStoryUnlocked);
    }
}
