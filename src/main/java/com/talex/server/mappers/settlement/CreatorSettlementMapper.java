package com.talex.server.mappers.settlement;

import com.talex.server.dtos.settlement.response.CreatorSettlementDetailResponseDto;
import com.talex.server.dtos.settlement.response.CreatorSettlementResponseDto;
import com.talex.server.entities.creator.CreatorMonthlySettlement;

public interface CreatorSettlementMapper {
    CreatorSettlementResponseDto toResponseDto(CreatorMonthlySettlement entity);

    CreatorSettlementDetailResponseDto toDetailResponseDto(CreatorMonthlySettlement entity);
}
