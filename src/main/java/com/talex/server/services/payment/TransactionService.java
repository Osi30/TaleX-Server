package com.talex.server.services.payment;

import com.talex.server.dtos.responses.payment.TransactionResponseDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.enums.transaction.ReferenceType;

import java.math.BigDecimal;
import java.util.List;

public interface TransactionService {
    Transaction createSuccessTransaction(Order order, BigDecimal paidAmount, PaymentMethod paymentMethod);
    List<TransactionResponseDto> getTransactionsByReference(ReferenceType referenceType, String referenceId);
}
