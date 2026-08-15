package com.talex.server.dtos.campaign.response;

import com.talex.server.enums.engagement.WalletReferenceType;
import com.talex.server.enums.engagement.WalletTransactionType;
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
public class CampaignWalletTransactionDto {
    private String transactionId;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private WalletTransactionType transactionType;
    private WalletReferenceType referenceType;
    private String referenceId;
    private String description;
    private LocalDateTime createdAt;
}