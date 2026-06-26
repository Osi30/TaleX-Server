package com.talex.server.services.creator;

import com.talex.server.dtos.responses.CreatorIdentityResponseDto;
import com.talex.server.dtos.requests.creator.CreatorIdentityRequestDto;
import com.talex.server.entities.Creator;

public interface ICreatorIdentityService {
    void create(Creator creator);

    CreatorIdentityResponseDto getById(String id);

    CreatorIdentityResponseDto getByAccountId(String creatorId);

    CreatorIdentityResponseDto update(String id, CreatorIdentityRequestDto dto);

    void delete(String id);
}
