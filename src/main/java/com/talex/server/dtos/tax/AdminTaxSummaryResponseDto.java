package com.talex.server.dtos.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminTaxSummaryResponseDto {
    // Thông tin Doanh nghiệp Demo
    private String companyName;
    private String enterpriseTaxCode;
    private String companyAddress;

    // Thống kê Thuế VAT
    private BigDecimal platformVatAmount; // PREMIUM, ENGAGEMENT
    private BigDecimal creatorVatAmount;  // EPISODE, COMBO
    private BigDecimal totalVatAmount;

    // Thống kê Thuế TNCN (PIT)
    private BigDecimal totalGrossAmount;   // Tổng doanh thu tính thuế của Creator
    private BigDecimal totalPitWithheld;   // Tổng thuế TNCN đã khấu trừ
    private BigDecimal totalNetPayout;     // Tổng tiền thực trả cho Creator
    private Integer totalSettlementsCount;
}