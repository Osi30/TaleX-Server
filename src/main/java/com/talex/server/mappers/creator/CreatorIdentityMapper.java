package com.talex.server.mappers.creator;

import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.responses.creator.CreatorIdentityResponseDto;
import com.talex.server.entities.creator.CreatorIdentity;

public interface CreatorIdentityMapper {
    CreatorIdentityResponseDto toResponseDto(CreatorIdentity entity);

    CreatorIdentity toEntity(CreatorIdentityRequestDto dto);
}
