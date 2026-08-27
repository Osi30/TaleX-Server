package com.talex.server.dtos.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MonthlyAccountSubscriptionResponseDto {
    private String accountSubscriptionId;
    private String subscriptionId;
    private String orderId;
    private Double totalAmount;
    private Double vatAmount;
    private Double amount;
    private String accountId;
    private String username;
    private String email;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Long totalViews;
    private Boolean isHasStat;
}