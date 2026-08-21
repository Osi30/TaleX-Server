package com.talex.server.mappers.settlement.impls;

import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.mappers.settlement.RevenueTransactionMapper;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class RevenueTransactionMapperImpl implements RevenueTransactionMapper {

    @Override
    public RevenueTransactionDto toDto(RevenueTransaction entity) {
        if (entity == null) return null;

        return RevenueTransactionDto.builder()
                .monthYear(entity.getMonthYear())
                .revenueTransactionId(entity.getRevenueTransactionId())
                .amount(entity.getAmount())
                .balanceBefore(entity.getBalanceBefore())
                .balanceAfter(entity.getBalanceAfter())
                .revenueTransactionType(entity.getRevenueTransactionType())
                .description(entity.getDescription())
                .createdAt(entity.getCreatedAt())
                .referenceType(entity.getReferenceType())
                .referenceId(entity.getReferenceId())
                .creatorId(entity.getCreator() != null ? entity.getCreator().getCreatorId() : null)
                .build();
    }

    @Override
    public List<RevenueTransactionDto> toListDto(List<RevenueTransaction> entities) {
        if (entities == null || entities.isEmpty()) {
            return List.of();
        }
        return entities.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }
}
