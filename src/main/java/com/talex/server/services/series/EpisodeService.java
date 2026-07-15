package com.talex.server.services.series;

import com.talex.server.dtos.requests.series.EpisodeRequestDto;
import com.talex.server.dtos.responses.series.EpisodeRefs;
import com.talex.server.dtos.requests.series.EpisodeUnlockSettingsRequestDto;
import com.talex.server.dtos.responses.series.EpisodeResponseDto;
import com.talex.server.entities.series.Episode;

import java.time.LocalDateTime;
import java.util.List;

public interface EpisodeService {
    EpisodeResponseDto create(String seasonId, EpisodeRequestDto request, String accountId);

    EpisodeResponseDto getById(String id, String accountId);

    EpisodeResponseDto getPublicById(String id);

    List<EpisodeResponseDto> listBySeason(String seasonId, String accountId);

    List<EpisodeResponseDto> listPublicBySeason(String seasonId);

    EpisodeResponseDto update(String id, EpisodeRequestDto request, String accountId);

    EpisodeResponseDto updateUnlockSettings(String id, EpisodeUnlockSettingsRequestDto request, String accountId);

    EpisodeResponseDto schedulePublish(String id, LocalDateTime scheduledPublishAt, String actorId);

    EpisodeResponseDto cancelSchedule(String id, String actorId);

    EpisodeResponseDto publish(String id, String actorId);

    EpisodeResponseDto publishScheduled(String id, String actorId);

    EpisodeResponseDto hide(String id, String actorId);

    EpisodeResponseDto unhide(String id, String actorId);

    EpisodeResponseDto forceHide(String id, String actorId);

    EpisodeResponseDto forceUnhide(String id, String actorId);

    void delete(String id, String actorId);

    Episode findActiveEntity(String id);

    Episode findPublicEntity(String id);

    EpisodeResponseDto toResponse(Episode episode);

    String getSeriesIdByEpisodeId(String episodeId);

    EpisodeRefs getEpisodeRefsByEpisodeId(String episodeId);
}
