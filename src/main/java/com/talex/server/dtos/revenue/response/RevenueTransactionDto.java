package com.talex.server.dtos.revenue.response;

import com.talex.server.enums.creator.RevenueTransactionType;
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
    private RevenueTransactionType revenueTransactionType;
    private String description;
    private LocalDate monthYear;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}