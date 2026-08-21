package com.talex.server.dtos.revenue.response;

import com.talex.server.enums.creator.RevenueTransactionType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueSummaryResponseDto {
    private BigDecimal totalRevenueAmount;     // PREMIUM_SHARE + CONTENT_SHARE
    private BigDecimal totalPenaltyAmount;     // PENALTY_DEDUCTION
    private BigDecimal totalAdjustmentAmount;  // ADJUSTMENT
    private Map<RevenueTransactionType, BigDecimal> amountByType; // Chi tiết gom theo từng type
}