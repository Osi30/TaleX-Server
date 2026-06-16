package com.talex.server.dtos.responses.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSubscriptionResponseDto {
    private String accountSubscriptionId;
    private String accountId;
    private String subscriptionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Boolean isAdBlocked;
    private Boolean isMovieUnlocked;
    private Boolean isStoryUnlocked;
    private Boolean isCancelled;
    private LocalDateTime updatedAt;
    private LocalDateTime cancelledAt;
}
