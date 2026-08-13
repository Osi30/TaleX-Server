package com.talex.server.mappers.settlement;

import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.entities.creator.RevenueTransaction;

import java.util.List;

public interface RevenueTransactionMapper {
    RevenueTransactionDto toDto(RevenueTransaction entity);
    List<RevenueTransactionDto> toListDto(List<RevenueTransaction> entities);
}
