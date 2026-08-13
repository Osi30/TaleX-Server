package com.talex.server.mappers.settlement;

import com.talex.server.dtos.settlement.response.PayoutTransactionDto;
import com.talex.server.entities.creator.PayoutTransaction;

import java.util.List;

public interface PayoutTransactionMapper {
    PayoutTransactionDto toDto(PayoutTransaction entity);
    List<PayoutTransactionDto> toListDto(List<PayoutTransaction> entities);
}
