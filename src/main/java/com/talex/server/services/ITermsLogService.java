package com.talex.server.services;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;

import java.util.List;

public interface ITermsLogService {
    void create(CreatorTermsLogRequestDto dto);

    CreatorTermsLogResponseDto getById(String id);

    List<CreatorTermsLogResponseDto> listByAccount(String creatorId);
}
