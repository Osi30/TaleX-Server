package com.talex.server.mappers;

import com.talex.server.dtos.requests.creator.CreatorTierRequestDto;
import com.talex.server.dtos.responses.CreatorTierResponseDto;
import com.talex.server.entities.creator.CreatorTier;

public interface ICreatorTierMapper {
    CreatorTierResponseDto toResponseDto(CreatorTier entity);

    CreatorTier toEntity(CreatorTierRequestDto dto);

    void updateEntity(CreatorTierRequestDto dto, CreatorTier entity);
}
