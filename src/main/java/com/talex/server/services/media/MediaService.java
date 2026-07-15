package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.media.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.media.MediaRejectRequestDto;
import com.talex.server.dtos.requests.media.MediaReorderRequestDto;
import com.talex.server.dtos.requests.media.MediaStatusRequestDto;
import com.talex.server.dtos.requests.media.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.media.CreatorViolationsSummaryDto;
import com.talex.server.dtos.responses.media.MediaResponseDto;
import com.talex.server.dtos.responses.media.MediaViolationsResponseDto;
import com.talex.server.entities.media.Media;
import org.springframework.data.domain.Page;

import java.util.List;

public interface MediaService {
    MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request, String accountId);

    List<MediaResponseDto> createComicPagesFromUrls(String episodeId, MediaComicPagesRequestDto request, String accountId);

    MediaResponseDto getById(String id, String accountId);

    MediaResponseDto getPublicById(String id);

    List<MediaResponseDto> listByEpisode(String episodeId, String accountId);

    List<MediaResponseDto> listPublicByEpisode(String episodeId, String viewerId);

    MediaResponseDto update(String id, MediaUpdateRequestDto request, String accountId);

    MediaResponseDto replaceUrl(String id, MediaMetadataRequestDto request, String accountId);

    List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request, String accountId);

    MediaResponseDto hide(String id, String actorId);

    MediaResponseDto unhide(String id, String actorId);

    MediaResponseDto forceHide(String id, String actorId);

    MediaResponseDto forceUnhide(String id, String actorId);

    MediaResponseDto approve(String id, String actorId);

    MediaResponseDto reject(String id, String actorId);

    CreatorViolationsSummaryDto getCreatorViolationsSummary(String creatorId);

    MediaResponseDto rejectWithReason(String id, String actorId, MediaRejectRequestDto request);

    MediaResponseDto updateProcessingStatus(String id, MediaStatusRequestDto request, String accountId);

    void delete(String id, String actorId);

    Media findActiveEntity(String id);

    Media findManageableEntity(String id, String accountId);

    MediaResponseDto toResponse(Media media);

    MediaViolationsResponseDto getMediaViolations(String mediaId);

    Page<MediaResponseDto> listPendingReview(int page, int size);
}
