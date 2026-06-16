package com.talex.server.dtos.requests.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AccountSubscriptionRequestDto {
    private UUID accountId;
    private String subscriptionId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
