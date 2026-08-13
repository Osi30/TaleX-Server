package com.talex.server.dtos.settlement.response;

import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.enums.transaction.SettlementStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorSettlementDetailResponseDto {
    private String creatorMonthlySettlementId;
    private String settlementMonth;
    private LocalDateTime cutoffDate;

    private BigDecimal grossAmount;
    private BigDecimal totalPenaltyAmount;
    private Double taxRate;
    private BigDecimal taxWithheldAmount;
    private BigDecimal netPayoutAmount;

    private SettlementStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    // Phân nhóm thông tin chi tiết lồng nhau[cite: 31]
    private CreatorDetailDto creatorDetail;
    private List<RevenueTransactionDto> revenueTransactions;
    private List<PayoutTransactionDto> payoutTransactions;
}