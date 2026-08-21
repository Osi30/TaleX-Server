package com.talex.server.dtos.statistics.subscription;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionRevenueOverviewDto {
    private BigDecimal totalGrossRevenue; // Tổng doanh thu (SUM totalAmount)
    private BigDecimal totalVatAmount;     // Tổng thuế VAT (SUM vatAmount)
    private BigDecimal totalNetRevenue;    // Doanh thu thuần (totalGrossRevenue - totalVatAmount)
}