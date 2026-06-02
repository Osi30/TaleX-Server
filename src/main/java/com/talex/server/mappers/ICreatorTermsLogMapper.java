package com.talex.server.mappers;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.CreatorTermsLog;

public interface ICreatorTermsLogMapper {
    CreatorTermsLogResponseDto toResponseDto(CreatorTermsLog entity);

    CreatorTermsLog toEntity(CreatorTermsLogRequestDto dto);
}
