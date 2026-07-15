package com.talex.server.mappers.creator;

import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;

public interface ICreatorMapper {
    CreatorResponseDto toResponseDto(Creator creator);
}
