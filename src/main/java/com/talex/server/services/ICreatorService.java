package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.CreatorRegisterDto;
import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;

import java.util.UUID;

public interface ICreatorService {
    String createCreator(CreatorRegisterDto dto);

    CreatorResponseDto getById(String id);

    CreatorResponseDto getByAccount(UUID accountId);

    BasePageResponse<CreatorResponseDto> filterCreators(CreatorFilterRequestDto filterRequest);

    CreatorResponseDto updateCreator(String id, CreatorRequestDto dto);

    void deleteCreator(String id);
}
