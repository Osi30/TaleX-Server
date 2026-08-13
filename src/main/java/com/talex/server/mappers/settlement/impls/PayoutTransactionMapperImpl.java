package com.talex.server.mappers.settlement.impls;

import com.talex.server.dtos.settlement.response.PayoutTransactionDto;
import com.talex.server.entities.creator.PayoutTransaction;
import com.talex.server.mappers.settlement.PayoutTransactionMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class PayoutTransactionMapperImpl implements PayoutTransactionMapper {

    @Override
    public PayoutTransactionDto toDto(PayoutTransaction entity) {
        if (entity == null) return null;

        return PayoutTransactionDto.builder()
                .payoutTransactionId(entity.getPayoutTransactionId())
                .batchReferenceId(entity.getBatchReferenceId())
                .transactionReferenceId(entity.getTransactionReferenceId())
                .gatewayBatchId(entity.getGatewayBatchId())
                .payoutReference(entity.getPayoutReference())
                .amount(entity.getAmount())
                .status(entity.getStatus())
                .failureReason(entity.getFailureReason())
                .paidAt(entity.getPaidAt())
                .toBin(entity.getToBin())
                .toAccountNumber(entity.getToAccountNumber())
                .toAccountName(entity.getToAccountName())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }

    @Override
    public List<PayoutTransactionDto> toListDto(List<PayoutTransaction> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
