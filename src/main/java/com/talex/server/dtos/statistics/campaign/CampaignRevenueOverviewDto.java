package com.talex.server.dtos.statistics.campaign;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CampaignRevenueOverviewDto {
    private BigDecimal totalGrossRevenue; // Tổng doanh thu (Sum totalAmount)
    private BigDecimal totalVatAmount;     // Tổng thuế VAT (Sum vatAmount)
    private BigDecimal totalNetRevenue;    // Doanh thu thuần (totalGrossRevenue - totalVatAmount)
}