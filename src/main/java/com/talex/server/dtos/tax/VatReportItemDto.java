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
public class VatReportItemDto {
    private String orderId;
    private String itemType;
    private String itemId;
    private BigDecimal fiatAmount;
    private BigDecimal totalAmount;
    private Double vatRate;
    private BigDecimal vatAmount;
    private String paymentCode;
    private LocalDateTime createdAt;
    // "PLATFORM" or "CREATOR"
    private String revenueGroup;
}