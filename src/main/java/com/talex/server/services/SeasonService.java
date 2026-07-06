package com.talex.server.services;

import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.dtos.responses.SeasonResponseDto;
import com.talex.server.entities.series.Season;

import java.util.List;

public interface SeasonService {
    SeasonResponseDto create(String seriesId, SeasonRequestDto request, String accountId);

    SeasonResponseDto getById(String id, String accountId);

    SeasonResponseDto getPublicById(String id);

    List<SeasonResponseDto> listBySeries(String seriesId, String accountId);

    List<SeasonResponseDto> listPublicBySeries(String seriesId);

    SeasonResponseDto update(String id, SeasonRequestDto request, String accountId);

    SeasonResponseDto hide(String id, String actorId);

    SeasonResponseDto unhide(String id, String actorId);

    SeasonResponseDto forceHide(String id, String actorId);

    SeasonResponseDto forceUnhide(String id, String actorId);

    void delete(String id, String actorId);

    Season findActiveEntity(String id);

    Season findPublicEntity(String id);

    SeasonResponseDto toResponse(Season season);
}
