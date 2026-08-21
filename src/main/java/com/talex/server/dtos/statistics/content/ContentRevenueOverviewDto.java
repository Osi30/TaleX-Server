package com.talex.server.dtos.statistics.content;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ContentRevenueOverviewDto {
    private BigDecimal totalGrossRevenue;        // Tổng doanh thu (SUM total_amount)
    private BigDecimal totalVatAmount;           // Tổng thuế VAT (SUM vat_amount)
    private BigDecimal totalCoinAmount;          // Tổng coin sử dụng (SUM coin_amount)
    private BigDecimal totalCreatorShareAmount;  // Tổng chia sẻ cho creator (SUM amount từ RevenueTransaction)
    private BigDecimal totalNetRevenue;          // Doanh thu thuần (Gross - VAT - Coin - Share)
}