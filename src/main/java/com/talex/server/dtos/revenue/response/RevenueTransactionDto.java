package com.talex.server.dtos.revenue.response;

import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.enums.transaction.ReferenceType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RevenueTransactionDto {
    private String revenueTransactionId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private RevenueTransactionType revenueTransactionType;
    private String description;
    private LocalDateTime createdAt;
    private LocalDate monthYear;
    private ReferenceType referenceType;
    private String referenceId;
    private String creatorId;
}