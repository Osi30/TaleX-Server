package com.talex.server.dtos.campaign.response;

import com.talex.server.enums.BankBin;
import com.talex.server.enums.PayoutStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPayoutTransactionResponseDto {
    private String walletPayoutTransactionId;
    private String batchReferenceId;
    private String transactionReferenceId;
    private String gatewayBatchId;
    private String payoutReference;
    private BigDecimal amount;
    private PayoutStatus status;
    private String failureReason;
    private LocalDateTime paidAt;
    private BankBin toBin;
    private String toAccountNumber;
    private String toAccountName;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}