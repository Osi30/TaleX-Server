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
public class ContentRevenueDetailDto {
    private String period;                 // Nhãn thời gian (VD: "2026-08-21 14:00", "2026-08-21", "2026-08", "2026")
    private BigDecimal grossRevenue;       // Doanh thu (SUM total_amount)
    private BigDecimal vatAmount;          // Thuế VAT (SUM vat_amount)
    private BigDecimal coinAmount;         // Số coin sử dụng (SUM coin_amount)
    private BigDecimal creatorShareAmount;  // Số tiền share (SUM amount từ RevenueTransaction)
    private BigDecimal netRevenue;         // Doanh thu ròng (Gross - VAT - Coin - Share)
    private String groupUnit;              // Đơn vị gom nhóm ("HOUR", "DAY", "MONTH", "YEAR")
}