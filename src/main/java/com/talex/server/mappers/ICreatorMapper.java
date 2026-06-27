package com.talex.server.mappers;

import com.talex.server.dtos.requests.creator.CreatorRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;

public interface ICreatorMapper {
    CreatorResponseDto toResponseDto(Creator creator);

    Creator toEntity(CreatorRequestDto dto);
}
