package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorRegisterDto;
import com.talex.server.dtos.requests.creator.CreatorRequestDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;

import java.util.UUID;

public interface ICreatorService {
    CreatorResponseDto createCreator(CreatorRegisterDto dto);

    CreatorResponseDto getById(String id);

    CreatorResponseDto getByAccount(UUID accountId);

    Creator getEntityByAccountId(UUID accountId);

    Creator getEntityById(String creatorId);

    BasePageResponse<CreatorResponseDto> filterCreators(CreatorFilterRequestDto filterRequest);

    CreatorResponseDto updateCreator(String id, CreatorRequestDto dto);

    void deleteCreator(String id);
}
