package com.talex.server.dtos.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionResultResponseDto {
    private String id;
    private Double alpha;
    private Double gamma;
    private Double subscriptionFee;
    private Double totalBudget;
    private Double targetBudget;
    private Double calculatedBudget;
    private String monthYear;
}