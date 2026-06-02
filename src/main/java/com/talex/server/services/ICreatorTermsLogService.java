package com.talex.server.services;

import com.talex.server.dtos.requests.CreatorTermsLogRequestDto;
import com.talex.server.dtos.responses.CreatorTermsLogResponseDto;

import java.util.List;

public interface ICreatorTermsLogService {
    CreatorTermsLogResponseDto create(CreatorTermsLogRequestDto dto);

    CreatorTermsLogResponseDto getById(String id);

    void delete(String id);

    List<CreatorTermsLogResponseDto> listByCreator(String creatorId);
}
