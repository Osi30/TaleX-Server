package com.talex.server.dtos.settlement.response;

import com.talex.server.enums.transaction.SettlementStatus;
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
public class CreatorSettlementResponseDto {
    private String creatorMonthlySettlementId;
    private String settlementMonth;
    private LocalDateTime cutoffDate;

    private BigDecimal grossAmount;
    private BigDecimal totalPenaltyAmount;
    private Double taxRate;
    private BigDecimal taxWithheldAmount;
    private BigDecimal netPayoutAmount;

    private SettlementStatus status;
    private String creatorId;
    private String creatorName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}