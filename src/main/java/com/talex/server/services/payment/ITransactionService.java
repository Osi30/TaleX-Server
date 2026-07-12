package com.talex.server.services.payment;

import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.PaymentMethod;

import java.math.BigDecimal;

public interface ITransactionService {
    Transaction createSuccessTransaction(Order order, BigDecimal paidAmount, PaymentMethod paymentMethod);
}
