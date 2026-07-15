package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorRegisterDto;
import com.talex.server.dtos.requests.filters.CreatorFilterRequestDto;
import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.records.CreatorVerificationStatus;

import java.util.UUID;

public interface ICreatorService {
    CreatorResponseDto createCreator(CreatorRegisterDto dto);

    String verifyCreator(CreatorRegisterDto dto);

    CreatorVerificationStatus checkAndGetVerificationStatus(UUID accountId);

    void sendUpdateRoleRequest(UUID accountId);

    CreatorResponseDto getById(String id);

    CreatorResponseDto getByAccount(UUID accountId);

    Creator getEntityByAccountId(UUID accountId);

    String getIdByAccountId(UUID accountId);

    Creator getEntityById(String creatorId);

    BasePageResponse<CreatorResponseDto> filterCreators(CreatorFilterRequestDto filterRequest);
}
