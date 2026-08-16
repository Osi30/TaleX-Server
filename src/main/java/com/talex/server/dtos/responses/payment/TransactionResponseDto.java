package com.talex.server.dtos.responses.payment;

import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.enums.transaction.TransactionStatus;
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
public class TransactionResponseDto {
    private String transactionId;
    private BigDecimal paidAmount;
    private PaymentMethod paymentMethod;
    private TransactionStatus status;
    private ReferenceType referenceType;
    private String referenceId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}