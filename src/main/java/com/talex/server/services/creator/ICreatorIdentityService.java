package com.talex.server.services.creator;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.dtos.requests.creator.CreatorVerifiedResultDto;
import com.talex.server.dtos.requests.filters.CreatorIdentityFilterRequestDto;
import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.entities.creator.Creator;

import java.util.UUID;

public interface ICreatorIdentityService {
    void create(Creator creator);

    CreatorIdentityResponseDto getById(String id);

    CreatorIdentityResponseDto getByAccountId(String creatorId);

    CreatorIdentityResponseDto update(String id, CreatorIdentityRequestDto dto);

    void updateVerifiedStatus(String id, CreatorVerifiedResultDto dto);

    String updateTaxId(UUID accountId, String taxId);

    void delete(String id);

    BasePageResponse<CreatorIdentityResponseDto> filter(CreatorIdentityFilterRequestDto filterRequest);
}
