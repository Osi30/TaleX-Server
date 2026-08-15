package com.talex.server.services.payment.impls;

import com.talex.server.dtos.responses.payment.TransactionResponseDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.enums.transaction.ReferenceType;
import com.talex.server.enums.transaction.TransactionStatus;
import com.talex.server.repositories.transaction.TransactionRepository;
import com.talex.server.services.payment.TransactionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
public class TransactionServiceImpl implements TransactionService {

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

    @Override
    @Transactional(readOnly = true)
    public List<TransactionResponseDto> getTransactionsByReference(ReferenceType referenceType, String referenceId) {
        return transactionRepository
                .findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(referenceType, referenceId)
                .stream()
                .map(this::toTransactionResponseDto)
                .toList();
    }

    private TransactionResponseDto toTransactionResponseDto(Transaction transaction) {
        return TransactionResponseDto.builder()
                .transactionId(transaction.getTransactionId())
                .paidAmount(transaction.getPaidAmount())
                .paymentMethod(transaction.getPaymentMethod())
                .status(transaction.getStatus())
                .referenceType(transaction.getReferenceType())
                .referenceId(transaction.getReferenceId())
                .createdAt(transaction.getCreatedAt())
                .updatedAt(transaction.getUpdatedAt())
                .build();
    }
}
