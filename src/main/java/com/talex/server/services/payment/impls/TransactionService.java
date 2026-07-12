package com.talex.server.services.payment.impls;

import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.enums.transaction.TransactionStatus;
import com.talex.server.repositories.transaction.TransactionRepository;
import com.talex.server.services.payment.ITransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class TransactionService implements ITransactionService {

    private final TransactionRepository transactionRepository;

    @Override
    @Transactional
    public Transaction createSuccessTransaction(Order order, BigDecimal paidAmount, PaymentMethod paymentMethod) {
        Transaction transaction = Transaction.builder()
                .paidAmount(paidAmount)
                .paymentMethod(paymentMethod)
                .status(TransactionStatus.SUCCESS)
                .referenceType(ReferenceType.ORDER)
                .referenceId(order.getOrderId())
                .build();

        return transactionRepository.save(transaction);
    }
}
