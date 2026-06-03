package com.talex.server.services;

import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.entities.Media;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface MediaService {
    MediaResponseDto upload(String episodeId, MultipartFile file, MediaMetadataRequestDto request);

    List<MediaResponseDto> uploadComicPages(
            String episodeId,
            List<MultipartFile> files,
            List<Integer> displayOrders,
            String actorId);

    MediaResponseDto getById(String id);

    MediaResponseDto getPublicById(String id);

    List<MediaResponseDto> listByEpisode(String episodeId);

    List<MediaResponseDto> listPublicByEpisode(String episodeId);

    MediaResponseDto update(String id, MediaUpdateRequestDto request);

    MediaResponseDto replaceFile(String id, MultipartFile file, MediaMetadataRequestDto request);

    List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request);

    MediaResponseDto hide(String id, String actorId);

    MediaResponseDto unhide(String id, String actorId);

    MediaResponseDto updateProcessingStatus(String id, MediaStatusRequestDto request);

    void delete(String id, String actorId);

    Media findActiveEntity(String id);

    MediaResponseDto toResponse(Media media);
}
