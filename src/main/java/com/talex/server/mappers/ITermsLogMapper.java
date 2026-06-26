package com.talex.server.mappers;

import com.talex.server.dtos.requests.terms.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;
import com.talex.server.entities.term.TermsLog;

public interface ITermsLogMapper {
    CreatorTermsLogResponseDto toResponseDto(TermsLog entity);

    TermsLog toEntity(CreatorTermsLogRequestDto dto);
}
