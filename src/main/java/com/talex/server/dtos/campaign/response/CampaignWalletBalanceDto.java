package com.talex.server.dtos.campaign.response;

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
public class CampaignWalletBalanceDto {
    private String walletId;
    private BigDecimal balance;
    private LocalDateTime updatedAt;
}