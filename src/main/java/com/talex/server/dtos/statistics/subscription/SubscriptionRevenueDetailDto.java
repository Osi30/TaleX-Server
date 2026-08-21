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
public class SubscriptionRevenueDetailDto {
    private String period;          // Nhãn thời gian (VD: "2026-08-21 14:00", "2026-08-21", "2026-08", "2026")
    private BigDecimal grossRevenue;// Doanh thu (SUM totalAmount)
    private BigDecimal vatAmount;   // Thuế VAT (SUM vatAmount)
    private BigDecimal netRevenue;  // Doanh thu ròng (totalAmount - vatAmount)
    private String groupUnit;       // Đơn vị gom nhóm ("HOUR", "DAY", "MONTH", "YEAR")
}