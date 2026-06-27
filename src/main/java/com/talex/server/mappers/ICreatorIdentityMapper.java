package com.talex.server.mappers;

import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.entities.creator.CreatorIdentity;

public interface ICreatorIdentityMapper {
    CreatorIdentityResponseDto toResponseDto(CreatorIdentity entity);

    CreatorIdentity toEntity(CreatorIdentityRequestDto dto);
}
