package com.talex.server.dtos.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorTaxSummaryResponseDto {
    private String creatorId;
    private String fullName;
    private String taxId;
    private String idNumber;
    private Integer taxYear;
    private BigDecimal totalGrossAmount;
    private BigDecimal totalPitWithheld;
    private BigDecimal totalNetPayout;
    private List<PitReportItemDto> monthlyDetails;
}