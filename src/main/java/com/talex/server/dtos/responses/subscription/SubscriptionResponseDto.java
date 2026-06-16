package com.talex.server.dtos.responses.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResponseDto {
    private String subscriptionId;
    private String tier;
    private String description;
    private BigDecimal price;
    private Integer duration;
    private String durationUnit;
    private BigDecimal totalPurchases;
    private Boolean isDeleted;
    private Boolean isAdBlocked;
    private Boolean isMovieUnlocked;
    private Boolean isStoryUnlocked;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
