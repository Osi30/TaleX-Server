package com.talex.server.dtos.settlement.response;

import com.talex.server.enums.BankBin;
import com.talex.server.enums.PayoutStatus;
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
public class PayoutTransactionDto {
    private String payoutTransactionId;

    // Mã đối soát PayOS & Hệ thống
    private String batchReferenceId;
    private String transactionReferenceId;
    private String gatewayBatchId;
    private String payoutReference; // Mã transactions[i].id từ PayOS

    // Thông tin số tiền & Trạng thái
    private BigDecimal amount;
    private PayoutStatus status;
    private String failureReason;
    private LocalDateTime paidAt;

    // Thông tin tài khoản nhận
    private BankBin toBin;
    private String toAccountNumber;
    private String toAccountName;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}