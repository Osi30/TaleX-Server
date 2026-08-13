package com.talex.server.dtos.tax;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PitReportItemDto {
    private String settlementId;
    private String settlementMonth;
    private String creatorId;
    private String creatorFullName;
    private String taxId;
    private String idNumber;
    private BigDecimal grossAmount;
    private Double taxRate;
    private BigDecimal taxWithheldAmount;
    private BigDecimal netPayoutAmount;
    private String status;
    private LocalDateTime createdAt;
}