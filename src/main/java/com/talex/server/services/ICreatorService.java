package com.talex.server.services;

import com.talex.server.dtos.requests.CreatorRegisterDto;
import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;

public interface ICreatorService {
    String createCreator(CreatorRegisterDto dto);

    CreatorResponseDto getById(String id);

    CreatorResponseDto updateCreator(String id, CreatorRequestDto dto);

    void deleteCreator(String id);
}
