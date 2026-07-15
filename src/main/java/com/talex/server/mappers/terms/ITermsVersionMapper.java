package com.talex.server.mappers.terms;

import com.talex.server.dtos.requests.terms.TermsVersionRequestDto;
import com.talex.server.dtos.responses.creator.TermsVersionResponseDto;
import com.talex.server.entities.term.TermsVersion;

public interface ITermsVersionMapper {
    TermsVersionResponseDto toResponseDto(TermsVersion entity);

    TermsVersion toEntity(TermsVersionRequestDto dto);
}
