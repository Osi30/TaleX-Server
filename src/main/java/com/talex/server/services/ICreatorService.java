package com.talex.server.services;

import com.talex.server.dtos.requests.CreatorRequestDto;
import com.talex.server.dtos.responses.CreatorResponseDto;

import java.util.List;
import java.util.Map;

public interface ICreatorService {
    CreatorResponseDto createCreator(CreatorRequestDto dto);

    CreatorResponseDto getById(String id);

    CreatorResponseDto updateCreator(String id, CreatorRequestDto dto);

    void deleteCreator(String id);

    List<CreatorResponseDto> listCreators(Map<String, Object> params);
}
