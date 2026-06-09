package com.talex.server.dtos.responses;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {
    private String subscriptionId;
    private String tier;
    private String description;
    private Long price;
    private Integer duration;
    private String durationUnit;
    private Long totalPurchases;
    private Boolean isDeleted;
    private Boolean isAdBlocked;
    private Boolean isMovieUnlocked;
    private Boolean isStoryUnlocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
