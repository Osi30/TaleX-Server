package com.talex.server.dtos.revenue.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTimeSeriesResponseDto {
    private String timePeriod;                 // Nhãn thời gian (VD: "2026-08-21 14:00", "2026-08-21", "2026-08", "2026")
    private BigDecimal totalRevenueAmount;     // PREMIUM_SHARE + CONTENT_SHARE
    private BigDecimal totalPenaltyAmount;     // PENALTY_DEDUCTION
    private BigDecimal totalAdjustmentAmount;  // ADJUSTMENT
    private String groupUnit;                  // "HOUR", "DAY", "MONTH", "YEAR"
}