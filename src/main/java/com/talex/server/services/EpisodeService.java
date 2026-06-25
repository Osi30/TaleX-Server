package com.talex.server.services;

import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.responses.EpisodeResponseDto;
import com.talex.server.entities.Episode;

import java.time.LocalDateTime;
import java.util.List;

public interface EpisodeService {
    EpisodeResponseDto create(String seasonId, EpisodeRequestDto request);

    EpisodeResponseDto getById(String id);

    EpisodeResponseDto getPublicById(String id);

    List<EpisodeResponseDto> listBySeason(String seasonId);

    List<EpisodeResponseDto> listPublicBySeason(String seasonId);

    EpisodeResponseDto update(String id, EpisodeRequestDto request);

    EpisodeResponseDto approve(String id, String actorId);

    EpisodeResponseDto reject(String id, String actorId);

    EpisodeResponseDto schedulePublish(String id, LocalDateTime scheduledPublishAt, String actorId);

    EpisodeResponseDto publish(String id, String actorId);

    EpisodeResponseDto publishScheduled(String id, String actorId);

    EpisodeResponseDto hide(String id, String actorId);

    EpisodeResponseDto unhide(String id, String actorId);

    void delete(String id, String actorId);

    Episode findActiveEntity(String id);

    Episode findPublicEntity(String id);

    EpisodeResponseDto toResponse(Episode episode);
}
