package com.talex.server.dtos.subscription.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorPoolSummaryResponseDto {
    private Integer year;
    private Integer month;
    private BigDecimal totalAmount;
    private BigDecimal taxAmount;
    private BigDecimal fiatAmount;
    private Long totalSubscriptions;
}