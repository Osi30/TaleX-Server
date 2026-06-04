package com.talex.server.services;

import com.talex.server.dtos.requests.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.entities.Media;

import java.util.List;

public interface MediaService {
    MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request);

    List<MediaResponseDto> createComicPagesFromUrls(String episodeId, MediaComicPagesRequestDto request);

    MediaResponseDto getById(String id);

    MediaResponseDto getPublicById(String id);

    List<MediaResponseDto> listByEpisode(String episodeId);

    List<MediaResponseDto> listPublicByEpisode(String episodeId);

    MediaResponseDto update(String id, MediaUpdateRequestDto request);

    MediaResponseDto replaceUrl(String id, MediaMetadataRequestDto request);

    List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request);

    MediaResponseDto hide(String id, String actorId);

    MediaResponseDto unhide(String id, String actorId);

    MediaResponseDto updateProcessingStatus(String id, MediaStatusRequestDto request);

    void delete(String id, String actorId);

    Media findActiveEntity(String id);

    MediaResponseDto toResponse(Media media);
}
