package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorTierRequestDto;
import com.talex.server.dtos.requests.filters.CreatorTierFilterRequestDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.entities.creator.CreatorTier;

public interface ICreatorTierService {
    CreatorTierResponseDto create(CreatorTierRequestDto dto);

    CreatorTierResponseDto getById(String id);

    CreatorTier getDefaultTier();

    CreatorTierResponseDto update(String id, CreatorTierRequestDto dto);

    void delete(String id);

    BasePageResponse<CreatorTierResponseDto> list(CreatorTierFilterRequestDto filterRequest);

    CreatorTier findById(String id);
}
