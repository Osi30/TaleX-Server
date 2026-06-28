package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.SeriesRequestDto;
import com.talex.server.dtos.responses.SeriesResponseDto;
import com.talex.server.entities.series.Series;

import java.util.UUID;

public interface SeriesService {
    SeriesResponseDto create(SeriesRequestDto request, UUID accountId);

    SeriesResponseDto getById(String id, String accountId);

    SeriesResponseDto getPublicById(String id);

    BasePageResponse<SeriesResponseDto> list(Integer page, Integer pageSize);

    BasePageResponse<SeriesResponseDto> listByCreator(UUID accountId, Integer page, Integer pageSize);

    BasePageResponse<SeriesResponseDto> listPublic(Integer page, Integer pageSize);

    SeriesResponseDto update(String id, SeriesRequestDto request, String accountId);

    SeriesResponseDto publish(String id, String actorId);

    SeriesResponseDto hide(String id, String actorId);

    SeriesResponseDto unhide(String id, String actorId);

    void delete(String id, String actorId);

    Series findActiveEntity(String id);

    Series findPublicEntity(String id);

    SeriesResponseDto toResponse(Series series);
}
