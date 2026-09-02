package com.talex.server.dtos.payout.response;

import com.talex.server.enums.BankBin;
import com.talex.server.enums.engagement.PayoutRequestStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PayoutRequestResponseDto {
    private String payoutRequestId;
    private UUID accountId;
    private String username;
    private BigDecimal amount;
    private PayoutRequestStatus status;
    private String paymentProfileId;
    private BankBin bankName;
    private String bankAccountNumber;
    private String bankAccountName;
    private String adminNote;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}