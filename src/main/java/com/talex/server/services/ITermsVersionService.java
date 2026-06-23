package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.TermsVersionRequestDto;
import com.talex.server.dtos.requests.filters.TermVersionFilterRequestDto;
import com.talex.server.dtos.responses.TermsVersionResponseDto;
import com.talex.server.entities.term.TermsVersion;
import com.talex.server.enums.TermsType;

public interface ITermsVersionService {
    TermsVersionResponseDto create(TermsVersionRequestDto dto);

    TermsVersionResponseDto getById(String id);

    TermsVersionResponseDto getActiveByType(TermsType type);

    TermsVersionResponseDto update(String id, TermsVersionRequestDto dto);

    void delete(String id);

    BasePageResponse<TermsVersionResponseDto> list(TermVersionFilterRequestDto filterRequest);

    TermsVersion findById(String id);
}
