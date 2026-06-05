package com.talex.server.services;

import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.dtos.responses.SeasonResponseDto;
import com.talex.server.entities.Season;

import java.util.List;

public interface SeasonService {
    SeasonResponseDto create(String seriesId, SeasonRequestDto request);

    SeasonResponseDto getById(String id);

    SeasonResponseDto getPublicById(String id);

    List<SeasonResponseDto> listBySeries(String seriesId);

    List<SeasonResponseDto> listPublicBySeries(String seriesId);

    SeasonResponseDto update(String id, SeasonRequestDto request);

    SeasonResponseDto publish(String id, String actorId);

    SeasonResponseDto hide(String id, String actorId);

    SeasonResponseDto unhide(String id, String actorId);

    void delete(String id, String actorId);

    Season findActiveEntity(String id);

    Season findPublicEntity(String id);

    SeasonResponseDto toResponse(Season season);
}
