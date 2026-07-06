package com.talex.server.services.impls;

import com.talex.server.dtos.requests.MediaComicPageRequestDto;
import com.talex.server.dtos.requests.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaRejectRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.ContentCensorshipResponseDto;
import com.talex.server.dtos.responses.MediaCopyrightResponseDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.dtos.responses.MediaViolationsResponseDto;
import com.talex.server.dtos.responses.ViolationDetailResponseDto;
import com.talex.server.entities.media.ContentCensorship;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaCopyright;
import com.talex.server.enums.media.CensorshipStatus;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.enums.media.MediaPlaybackPolicy;
import com.talex.server.enums.media.MediaProtectionType;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.ContentCensorshipRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.MediaCopyrightRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.ContentOwnershipService;
import com.talex.server.services.ContentPipelineService;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.MediaPlaybackSecurityService;
import com.talex.server.services.MediaService;
import com.talex.server.services.media.MediaProviderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaServiceImpl implements MediaService {
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final String URL_STORAGE_PROVIDER = "URL";
    private static final List<MediaStatus> PUBLIC_READY_STATUSES = List.of(MediaStatus.ACTIVE, MediaStatus.HLS_READY);
    private static final List<MediaStatus> VIDEO_REPLACEMENT_BLOCKING_STATUSES = List.of(
            MediaStatus.ACTIVE,
            MediaStatus.HLS_READY,
            MediaStatus.HLS_PROCESSING);

    private final MediaRepository mediaRepository;
    private final EpisodeRepository episodeRepository;
    private final EpisodeService episodeService;
    private final MediaProviderService mediaProviderService;
    private final MediaPlaybackSecurityService playbackSecurityService;
    private final ContentPipelineService contentPipelineService;
    private final MediaCopyrightRepository mediaCopyrightRepository;
    private final ContentCensorshipRepository contentCensorshipRepository;
    private final ContentOwnershipService contentOwnershipService;

    private record PreparedMediaUrl(
            String fileUrl,
            String checksum,
            String mimeType,
            Long fileSize,
            String storageProvider,
            MediaProvider provider) {
    }

    @Transactional
    @Override
    public MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request, String accountId) {
        Episode episode = lockActiveEpisode(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        validateEpisodeStatusForMediaModification(episode);
        if (request == null) {
            throw ContentModuleException.badRequest("Media URL request is required");
        }
        MediaType mediaType = resolveMediaType(episode, request.getMediaType());
        return createOneFromUrl(episode, request, mediaType, request.getDisplayOrder());
    }

    @Transactional
    @Override
    public List<MediaResponseDto> createComicPagesFromUrls(String episodeId, MediaComicPagesRequestDto request, String accountId) {
        Episode episode = lockActiveEpisode(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        validateEpisodeStatusForMediaModification(episode);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Batch media URL creation is only supported for comic episodes");
        }
        if (request == null || request.getPages() == null || request.getPages().isEmpty()) {
            throw ContentModuleException.badRequest("At least one comic page is required");
        }

        List<Integer> resolvedOrders = resolveComicDisplayOrders(episode.getEpisodeId(), request.getPages());
        Set<String> requestedChecksums = new HashSet<>();
        List<MediaMetadataRequestDto> metadataRequests = new ArrayList<>();
        List<PreparedMediaUrl> preparedUrls = new ArrayList<>();

        for (MediaComicPageRequestDto page : request.getPages()) {
            MediaMetadataRequestDto metadata = new MediaMetadataRequestDto();
            metadata.setFileUrl(page.getFileUrl());
            metadata.setOriginalUrl(page.getFileUrl());
            metadata.setMimeType(page.getMimeType());
            metadata.setFileSize(page.getFileSize());
            metadata.setChecksum(page.getChecksum());
            metadata.setExternalPublicId(page.getExternalPublicId());
            metadata.setStorageProvider(page.getStorageProvider());
            metadata.setWidth(page.getWidth());
            metadata.setHeight(page.getHeight());
            metadata.setResolution(page.getResolution());
            metadata.setActorId(request.getActorId());
            PreparedMediaUrl preparedUrl = prepareUrl(metadata, MediaType.IMAGE);
            if (!requestedChecksums.add(preparedUrl.checksum())) {
                throw ContentModuleException.conflict("Duplicate media URL detected");
            }
            metadataRequests.add(metadata);
            preparedUrls.add(preparedUrl);
        }

        rejectDuplicateChecksums(requestedChecksums, null);

        List<Media> mediaList = new ArrayList<>();
        for (int i = 0; i < metadataRequests.size(); i++) {
            Media media = new Media();
            media.setEpisode(episode);
            media.setCreatorId(episode.getCreatorId());
            media.setMediaType(MediaType.IMAGE);
            media.setDisplayOrder(resolvedOrders.get(i));
            applyPreparedUrl(media, metadataRequests.get(i), MediaType.IMAGE, preparedUrls.get(i));
            media.markCreatedBy(request.getActorId());
            mediaList.add(media);
        }

        List<Media> saved = mediaRepository.saveAll(mediaList);

        // Dispatch pipeline for each image — runs async via Kafka, doesn't block response
        for (Media media : saved) {
            try {
                contentPipelineService.dispatchPipelineJob(media);
            } catch (Exception e) {
                log.error("Failed to dispatch pipeline job for image mediaId={}", media.getMediaId(), e);
            }
        }

        return saved.stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponseDto getById(String id, String accountId) {
        return toResponse(findManageableEntity(id, accountId));
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponseDto getPublicById(String id) {
        Media media = findActiveEntity(id);
        if (!PUBLIC_READY_STATUSES.contains(media.getStatus())
                || media.getApprovalStatus() != ContentApprovalStatus.APPROVED) {
            throw ContentModuleException.notFound("Public media not found: " + id);
        }
        episodeService.findPublicEntity(media.getEpisode().getEpisodeId());
        return toPublicResponse(media);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MediaResponseDto> listByEpisode(String episodeId, String accountId) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        return mediaRepository.findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId)
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    @Override
    public List<MediaResponseDto> listPublicByEpisode(String episodeId) {
        episodeService.findPublicEntity(episodeId);
        return mediaRepository
                .findAllByEpisode_EpisodeIdAndStatusInAndApprovalStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
                        episodeId,
                        PUBLIC_READY_STATUSES,
                        ContentApprovalStatus.APPROVED)
                .stream()
                .map(this::toPublicResponse)
                .sorted(Comparator.comparing(MediaResponseDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Transactional
    @Override
    public MediaResponseDto update(String id, MediaUpdateRequestDto request, String accountId) {
        Media media = findManageableEntity(id, accountId);
        if (request.getWidth() != null) {
            media.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            media.setHeight(request.getHeight());
        }
        if (request.getResolution() != null) {
            media.setResolution(request.getResolution());
        }
        if (request.getDuration() != null) {
            media.setDuration(request.getDuration());
        }
        if (request.getDisplayOrder() != null) {
            ensureImageMedia(media);
            Episode episode = lockActiveEpisode(media.getEpisode().getEpisodeId());
            ensureDisplayOrderAvailable(
                    episode.getEpisodeId(),
                    request.getDisplayOrder(),
                    media.getMediaId());
            media.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            media.setStatus(request.getStatus());
        }
        media.markUpdatedBy(accountId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto replaceUrl(String id, MediaMetadataRequestDto request, String accountId) {
        Media media = findManageableEntity(id, accountId);
        Episode episode = lockActiveEpisode(media.getEpisode().getEpisodeId());
        MediaType mediaType = resolveMediaType(
                episode,
                request != null && request.getMediaType() != null ? request.getMediaType() : media.getMediaType());
        if (media.getMediaType() != mediaType) {
            throw ContentModuleException.badRequest("Replacement URL type must match existing media type");
        }
        validateMediaForEpisode(episode, mediaType, media.getMediaId());
        applyUrl(media, request, mediaType);
        media.markUpdatedBy(accountId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request, String accountId) {
        Episode episode = lockActiveEpisode(episodeId);
        contentOwnershipService.assertCanManage(episode, accountId);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Only comic episode media can be reordered");
        }
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            throw ContentModuleException.badRequest("At least one media reorder item is required");
        }
        if (request.getItems().stream().anyMatch(Objects::isNull)) {
            throw ContentModuleException.badRequest("Media reorder item must not be null");
        }

        Set<String> mediaIds = new LinkedHashSet<>();
        Set<Integer> displayOrders = new LinkedHashSet<>();
        for (var item : request.getItems()) {
            if (item.getMediaId() == null || item.getMediaId().isBlank()) {
                throw ContentModuleException.badRequest("mediaId is required");
            }
            if (!mediaIds.add(item.getMediaId().trim())) {
                throw ContentModuleException.badRequest("mediaId must not contain duplicates");
            }
            if (item.getDisplayOrder() == null || item.getDisplayOrder() < 1) {
                throw ContentModuleException.badRequest("displayOrder must be positive numbers");
            }
            if (!displayOrders.add(item.getDisplayOrder())) {
                throw ContentModuleException.badRequest("displayOrder must not contain duplicates");
            }
        }

        Map<String, Media> mediaById = mediaRepository.findAllByMediaIdInAndIsDeletedFalse(mediaIds)
                .stream()
                .collect(Collectors.toMap(Media::getMediaId, Function.identity()));
        if (mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndMediaIdNotInAndIsDeletedFalse(
                episodeId,
                displayOrders,
                mediaIds)) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }

        List<Media> changedMedia = new ArrayList<>();
        for (var item : request.getItems()) {
            Media media = mediaById.get(item.getMediaId().trim());
            if (media == null) {
                throw ContentModuleException.notFound("Media not found: " + item.getMediaId());
            }
            if (!media.getEpisode().getEpisodeId().equals(episodeId)) {
                throw ContentModuleException.badRequest("Media does not belong to episode: " + item.getMediaId());
            }
            ensureImageMedia(media);
            media.setDisplayOrder(item.getDisplayOrder());
            media.markUpdatedBy(accountId);
            changedMedia.add(media);
        }
        mediaRepository.saveAll(changedMedia);

        return listByEpisode(episodeId, accountId);
    }

    @Transactional
    @Override
    public MediaResponseDto hide(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        media.setStatus(MediaStatus.HIDDEN);
        media.markUpdatedBy(actorId);
        playbackSecurityService.revokeActiveSessions(media);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto unhide(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        media.setStatus(MediaStatus.ACTIVE);
        media.markUpdatedBy(actorId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto approve(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setApprovalStatus(ContentApprovalStatus.APPROVED);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.setApprovalReviewedBy(actorId);
        media.markUpdatedBy(actorId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto reject(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setApprovalStatus(ContentApprovalStatus.REJECTED);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.setApprovalReviewedBy(actorId);
        if (media.getStatus() == MediaStatus.ACTIVE || media.getStatus() == MediaStatus.HLS_READY) {
            media.setStatus(MediaStatus.HIDDEN);
            playbackSecurityService.revokeActiveSessions(media);
        }
        media.markUpdatedBy(actorId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto rejectWithReason(String id, String actorId, MediaRejectRequestDto request) {
        Media media = findActiveEntity(id);
        media.setApprovalStatus(ContentApprovalStatus.REJECTED);
        media.setApprovalReviewedAt(LocalDateTime.now());
        media.setApprovalReviewedBy(actorId);
        if (media.getStatus() == MediaStatus.ACTIVE || media.getStatus() == MediaStatus.HLS_READY) {
            media.setStatus(MediaStatus.HIDDEN);
            playbackSecurityService.revokeActiveSessions(media);
        }
        media.markUpdatedBy(actorId);
        mediaRepository.save(media);

        // Record human review decision as a censorship entry with staff's reason
        String reason = (request != null && request.getReason() != null) ? request.getReason() : "";
        ContentCensorship censorship = new ContentCensorship();
        censorship.setMedia(media);
        censorship.setReviewedBy("HUMAN");
        censorship.setReviewerNotes(reason);
        censorship.setStatus(CensorshipStatus.REJECTED);
        censorship.setCheckedAt(LocalDateTime.now());
        contentCensorshipRepository.save(censorship);

        return toResponse(media);
    }

    @Transactional
    @Override
    public MediaResponseDto updateProcessingStatus(String id, MediaStatusRequestDto request, String accountId) {
        Media media = findManageableEntity(id, accountId);
        if (request.getStatus() == MediaStatus.DELETED) {
            media.setStatus(MediaStatus.DELETED);
            media.softDelete(accountId);
            playbackSecurityService.revokeActiveSessions(media);
        } else {
            media.setStatus(request.getStatus());
            media.markUpdatedBy(accountId);
        }
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Media media = findManageableEntity(id, actorId);
        validateEpisodeStatusForMediaModification(media.getEpisode());
        media.setStatus(MediaStatus.DELETED);
        media.softDelete(actorId);
        playbackSecurityService.revokeActiveSessions(media);
        if (media.getMediaType() == MediaType.VIDEO && media.getProviderPublicId() != null) {
            mediaProviderService.deleteAsset(media);
        }
        mediaRepository.save(media);
    }

    @Override
    public Media findActiveEntity(String id) {
        return mediaRepository.findByMediaIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Media not found: " + id));
    }

    @Override
    public Media findManageableEntity(String id, String accountId) {
        if (contentOwnershipService.isPrivileged()) {
            return findActiveEntity(id);
        }

        String creatorId = contentOwnershipService.requireCurrentCreatorId(accountId);
        Media media = mediaRepository
                .findByMediaIdAndCreatorIdAndIsDeletedFalse(id, creatorId)
                .orElseThrow(() -> ContentModuleException.notFound("Media not found: " + id));
        contentOwnershipService.assertOwnedByCreator(media, creatorId);
        return media;
    }

    @Transactional(readOnly = true)
    @Override
    public MediaViolationsResponseDto getMediaViolations(String mediaId) {
        Media media = findActiveEntity(mediaId);

        List<MediaCopyright> copyrightEntities = mediaCopyrightRepository.findAllByMedia_MediaId(mediaId);
        List<ContentCensorship> censorshipEntities = contentCensorshipRepository.findAllByMedia_MediaId(mediaId);

        List<MediaCopyrightResponseDto> copyrightDtos = copyrightEntities.stream()
                .map(this::mapCopyrightToDto)
                .toList();

        List<ContentCensorshipResponseDto> censorshipDtos = censorshipEntities.stream()
                .map(this::mapCensorshipToDto)
                .toList();

        return MediaViolationsResponseDto.builder()
                .mediaId(media.getMediaId())
                .contentId(media.getProviderPublicId())
                .copyrightViolations(copyrightDtos)
                .censorshipResults(censorshipDtos)
                .build();
    }

    @Transactional(readOnly = true)
    @Override
    public Page<MediaResponseDto> listPendingReview(int page, int size) {
        PageRequest pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        return mediaRepository
                .findByApprovalStatusAndIsDeletedFalse(ContentApprovalStatus.PENDING_REVIEW, pageable)
                .map(this::toResponse);
    }

    @Override
    public MediaResponseDto toResponse(Media media) {
        return baseResponse(media).build();
    }

    private void validateEpisodeStatusForMediaModification(Episode episode) {
        if (episode.getStatus() != EpisodeStatus.DRAFT && episode.getStatus() != EpisodeStatus.HIDDEN) {
            throw ContentModuleException.badRequest("Cannot modify media when episode status is " + episode.getStatus());
        }
    }

    private MediaResponseDto toPublicResponse(Media media) {
        MediaResponseDto.MediaResponseDtoBuilder builder = baseResponse(media);
        boolean protectedVideo = media.getMediaType() == MediaType.VIDEO
                && media.getProtectionType() != MediaProtectionType.NONE
                && media.getPlaybackPolicy() != MediaPlaybackPolicy.PUBLIC;
        if (protectedVideo) {
            builder.fileUrl(null)
                    .originalUrl(null)
                    .hlsUrl(null)
                    .playbackUrl(null)
                    .signedPlaybackUrl(null);
        }
        return builder.build();
    }

    private MediaResponseDto.MediaResponseDtoBuilder baseResponse(Media media) {
        return MediaResponseDto.builder()
                .mediaId(media.getMediaId())
                .episodeId(media.getEpisode().getEpisodeId())
                .creatorId(media.getCreatorId())
                .mediaType(media.getMediaType())
                .mimeType(media.getMimeType())
                .fileUrl(media.getFileUrl())
                .externalPublicId(media.getExternalPublicId())
                .storageProvider(media.getStorageProvider())
                .provider(media.getProvider())
                .providerAssetId(media.getProviderAssetId())
                .providerPublicId(media.getProviderPublicId())
                .providerDeliveryType(media.getProviderDeliveryType())
                .originalUrl(media.getOriginalUrl())
                .playbackUrl(media.getPlaybackUrl())
                .hlsUrl(media.getHlsUrl())
                .signedPlaybackUrl(media.getSignedPlaybackUrl())
                .thumbnailUrl(media.getThumbnailUrl())
                .previewUrl(media.getPreviewUrl())
                .format(media.getFormat())
                .protectionType(media.getProtectionType())
                .playbackPolicy(media.getPlaybackPolicy())
                .drmProvider(media.getDrmProvider())
                .drmKeyId(media.getDrmKeyId())
                .drmLicenseUrl(media.getDrmLicenseUrl())
                .drmCertificateUrl(media.getDrmCertificateUrl())
                .tokenTtlSeconds(media.getTokenTtlSeconds())
                .errorMessage(media.getErrorMessage())
                .pendingDelete(media.getPendingDelete())
                .fileSize(media.getFileSize())
                .checksum(media.getChecksum())
                .width(media.getWidth())
                .height(media.getHeight())
                .resolution(media.getResolution())
                .duration(media.getDuration())
                .displayOrder(media.getDisplayOrder())
                .status(media.getStatus())
                .approvalStatus(media.getApprovalStatus())
                .approvalReviewedAt(media.getApprovalReviewedAt())
                .approvalReviewedBy(media.getApprovalReviewedBy())
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .deletedAt(media.getDeletedAt())
                .createdBy(media.getCreatedBy())
                .updatedBy(media.getUpdatedBy())
                .deletedBy(media.getDeletedBy())
                .isDeleted(media.getIsDeleted());
    }

    private MediaCopyrightResponseDto mapCopyrightToDto(MediaCopyright entity) {
        return MediaCopyrightResponseDto.builder()
                .mediaCopyrightId(entity.getMediaCopyrightId())
                .mediaId(entity.getMedia().getMediaId())
                .sourceMediaId(entity.getSourceMedia() != null ? entity.getSourceMedia().getMediaId() : null)
                .startTimeTarget(entity.getStartTimeTarget())
                .endTimeTarget(entity.getEndTimeTarget())
                .startTimeSource(entity.getStartTimeSource())
                .endTimeSource(entity.getEndTimeSource())
                .similarityScore(entity.getSimilarityScore())
                .violationType(entity.getViolationType())
                .isValid(entity.getIsValid())
                .note(entity.getNote())
                .checkedAt(entity.getCheckedAt())
                .build();
    }

    private ContentCensorshipResponseDto mapCensorshipToDto(ContentCensorship entity) {
        List<ViolationDetailResponseDto> details = entity.getViolationDetails() == null
                ? List.of()
                : entity.getViolationDetails().stream()
                        .map(vd -> ViolationDetailResponseDto.builder()
                                .violationDetailId(vd.getViolationDetailId())
                                .violationAt(vd.getViolationAt())
                                .endViolationAt(vd.getEndViolationAt())
                                .label(vd.getLabel())
                                .confidence(vd.getConfidence())
                                .suggestion(vd.getSuggestion())
                                .build())
                        .toList();

        return ContentCensorshipResponseDto.builder()
                .censorshipId(entity.getCensorshipId())
                .mediaId(entity.getMedia().getMediaId())
                .primaryViolationLabel(entity.getPrimaryViolationLabel())
                .confidenceScore(entity.getConfidenceScore())
                .checkedAt(entity.getCheckedAt())
                .reviewedBy(entity.getReviewedBy())
                .reviewerNotes(entity.getReviewerNotes())
                .status(entity.getStatus())
                .violationDetails(details)
                .build();
    }

    private MediaResponseDto createOneFromUrl(
            Episode episode,
            MediaMetadataRequestDto request,
            MediaType mediaType,
            Integer requestedDisplayOrder) {

        validateMediaForEpisode(episode, mediaType, null);

        Media media = new Media();
        media.setEpisode(episode);
        media.setCreatorId(episode.getCreatorId());
        media.setMediaType(mediaType);
        media.setDisplayOrder(resolveDisplayOrder(episode.getEpisodeId(), mediaType, requestedDisplayOrder));
        applyUrl(media, request, mediaType);
        media.markCreatedBy(request.getActorId());

        Media saved = mediaRepository.save(media);

        // Dispatch pipeline for IMAGE media — runs async via Kafka, doesn't block response
        if (mediaType == MediaType.IMAGE) {
            try {
                contentPipelineService.dispatchPipelineJob(saved);
            } catch (Exception e) {
                log.error("Failed to dispatch pipeline job for image mediaId={}", saved.getMediaId(), e);
            }
        }

        return toResponse(saved);
    }

    private void applyUrl(Media media, MediaMetadataRequestDto request, MediaType mediaType) {
        PreparedMediaUrl preparedUrl = prepareUrl(request, mediaType);
        rejectDuplicate(preparedUrl.checksum(), media.getMediaId());
        applyPreparedUrl(media, request, mediaType, preparedUrl);
    }

    private PreparedMediaUrl prepareUrl(MediaMetadataRequestDto request, MediaType mediaType) {
        if (request == null) {
            throw ContentModuleException.badRequest("Media URL request is required");
        }

        String fileUrl = normalizeFileUrl(request.getFileUrl());
        String checksum = normalizeChecksum(request.getChecksum(), fileUrl);
        String mimeType = validateMimeType(request.getMimeType(), mediaType);
        Long fileSize = validateFileSize(request.getFileSize());
        String storageProvider = normalizeStorageProvider(request.getStorageProvider());
        MediaProvider provider = request.getProvider() != null
                ? request.getProvider()
                : resolveProvider(request.getStorageProvider());
        return new PreparedMediaUrl(fileUrl, checksum, mimeType, fileSize, storageProvider, provider);
    }

    private void applyPreparedUrl(
            Media media,
            MediaMetadataRequestDto request,
            MediaType mediaType,
            PreparedMediaUrl preparedUrl) {
        media.setFileUrl(preparedUrl.fileUrl());
        media.setOriginalUrl(firstNonBlank(request.getOriginalUrl(), preparedUrl.fileUrl()));
        media.setPlaybackUrl(firstNonBlank(request.getPlaybackUrl(), request.getHlsUrl(), preparedUrl.fileUrl()));
        media.setHlsUrl(request.getHlsUrl());
        media.setThumbnailUrl(request.getThumbnailUrl());
        media.setPreviewUrl(request.getPreviewUrl());
        media.setChecksum(preparedUrl.checksum());
        media.setMimeType(preparedUrl.mimeType());
        media.setFileSize(preparedUrl.fileSize());
        media.setExternalPublicId(blankToNull(request.getExternalPublicId()));
        media.setStorageProvider(preparedUrl.storageProvider());
        media.setProvider(preparedUrl.provider());
        media.setProviderAssetId(blankToNull(request.getProviderAssetId()));
        media.setProviderPublicId(firstNonBlank(request.getProviderPublicId(), request.getExternalPublicId()));
        media.setProviderDeliveryType(blankToNull(request.getProviderDeliveryType()));
        media.setFormat(blankToNull(request.getFormat()));
        media.setProtectionType(request.getProtectionType() != null ? request.getProtectionType() : MediaProtectionType.NONE);
        media.setPlaybackPolicy(request.getPlaybackPolicy() != null ? request.getPlaybackPolicy() : MediaPlaybackPolicy.PUBLIC);
        media.setStatus(MediaStatus.ACTIVE);
        media.setApprovalStatus(ContentApprovalStatus.PENDING_REVIEW);
        media.setApprovalReviewedAt(null);
        media.setApprovalReviewedBy(null);
        applyMetadata(media, request, mediaType);
    }

    private void applyMetadata(Media media, MediaMetadataRequestDto request, MediaType mediaType) {
        if (request.getWidth() != null) {
            media.setWidth(request.getWidth());
        }
        if (request.getHeight() != null) {
            media.setHeight(request.getHeight());
        }
        if (request.getResolution() != null) {
            media.setResolution(request.getResolution());
        }
        if (request.getDuration() != null) {
            media.setDuration(request.getDuration());
        }
        if (mediaType == MediaType.IMAGE && request.getDisplayOrder() != null) {
            media.setDisplayOrder(request.getDisplayOrder());
        }
    }

    private MediaType resolveMediaType(Episode episode, MediaType requestedType) {
        MediaType mediaType = requestedType;
        if (mediaType == null) {
            mediaType = episode.getContentType() == ContentType.VIDEO ? MediaType.VIDEO : MediaType.IMAGE;
        }

        if (episode.getContentType() == ContentType.VIDEO && mediaType != MediaType.VIDEO) {
            throw ContentModuleException.badRequest("Video episode only accepts video media URL");
        }
        if (episode.getContentType() == ContentType.COMIC && mediaType != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("Comic episode only accepts image media URLs");
        }
        return mediaType;
    }

    private void validateMediaForEpisode(Episode episode, MediaType mediaType, String currentMediaId) {
        resolveMediaType(episode, mediaType);
        if (episode.getContentType() == ContentType.VIDEO) {
            boolean hasAnotherReadyVideo = currentMediaId == null
                    ? mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                            episode.getEpisodeId(),
                            MediaType.VIDEO,
                            VIDEO_REPLACEMENT_BLOCKING_STATUSES)
                    : mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalseAndMediaIdNot(
                            episode.getEpisodeId(),
                            MediaType.VIDEO,
                            VIDEO_REPLACEMENT_BLOCKING_STATUSES,
                            currentMediaId);
            if (hasAnotherReadyVideo) {
                throw ContentModuleException.conflict(
                        "Episode already has an active or processing video media. Delete it first.");
            }
        }
    }

    private String normalizeFileUrl(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) {
            throw ContentModuleException.badRequest("fileUrl is required");
        }

        String normalizedUrl = fileUrl.trim();
        try {
            URI uri = new URI(normalizedUrl);
            String scheme = uri.getScheme();
            if (scheme == null
                    || (!scheme.equalsIgnoreCase("http") && !scheme.equalsIgnoreCase("https"))
                    || uri.getHost() == null) {
                throw ContentModuleException.badRequest("fileUrl must be a valid HTTP or HTTPS URL");
            }
            return normalizedUrl;
        } catch (URISyntaxException exception) {
            throw ContentModuleException.badRequest("fileUrl must be a valid HTTP or HTTPS URL");
        }
    }

    private String validateMimeType(String mimeType, MediaType mediaType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw ContentModuleException.badRequest("mimeType is required");
        }
        String normalizedMimeType = mimeType.trim().toLowerCase(Locale.ROOT);
        if (mediaType == MediaType.VIDEO) {
            if (!normalizedMimeType.startsWith("video/")
                    && !"application/vnd.apple.mpegurl".equals(normalizedMimeType)) {
                throw ContentModuleException.badRequest("mimeType must be a video MIME type");
            }
            return normalizedMimeType;
        }

        if (!normalizedMimeType.startsWith("image/")) {
            throw ContentModuleException.badRequest("mimeType must be an image MIME type");
        }
        return normalizedMimeType;
    }

    private Long validateFileSize(Long fileSize) {
        if (fileSize == null) {
            throw ContentModuleException.badRequest("fileSize is required");
        }
        if (fileSize < 0) {
            throw ContentModuleException.badRequest("fileSize must be zero or positive");
        }
        return fileSize;
    }

    private void rejectDuplicate(String checksum, String currentMediaId) {
        mediaRepository.findFirstByChecksumAndIsDeletedFalse(checksum)
                .filter(existing -> !existing.getMediaId().equals(currentMediaId))
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Duplicate media URL detected");
                });
    }

    private void rejectDuplicateChecksums(Collection<String> checksums, String currentMediaId) {
        if (checksums.isEmpty()) {
            return;
        }

        mediaRepository.findAllByChecksumInAndIsDeletedFalse(checksums)
                .stream()
                .filter(existing -> currentMediaId == null || !existing.getMediaId().equals(currentMediaId))
                .findAny()
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Duplicate media URL detected");
                });
    }

    private String checksum(String value) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] bytes = digest.digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(bytes);
        } catch (NoSuchAlgorithmException exception) {
            throw new ContentModuleException(
                    4301,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot generate URL checksum",
                    exception);
        }
    }

    private String normalizeChecksum(String providedChecksum, String fallbackValue) {
        if (providedChecksum == null || providedChecksum.isBlank()) {
            return checksum(fallbackValue);
        }

        String normalizedChecksum = providedChecksum.trim().toLowerCase(Locale.ROOT);
        if (!SHA256_PATTERN.matcher(normalizedChecksum).matches()) {
            throw ContentModuleException.badRequest("checksum must be a SHA-256 hex string");
        }
        return normalizedChecksum;
    }

    private Integer resolveDisplayOrder(String episodeId, MediaType mediaType, Integer requestedDisplayOrder) {
        if (mediaType == MediaType.VIDEO) {
            return null;
        }
        if (requestedDisplayOrder != null) {
            ensureDisplayOrderAvailable(episodeId, requestedDisplayOrder, null);
            return requestedDisplayOrder;
        }
        return nextDisplayOrder(episodeId);
    }

    private List<Integer> resolveComicDisplayOrders(String episodeId, List<MediaComicPageRequestDto> pages) {
        if (pages.stream().anyMatch(page -> page.getDisplayOrder() == null)) {
            throw ContentModuleException.badRequest("displayOrder is required for every comic page");
        }

        List<Integer> displayOrders = pages.stream()
                .map(MediaComicPageRequestDto::getDisplayOrder)
                .toList();
        Set<Integer> uniqueOrders = new HashSet<>(displayOrders);
        if (uniqueOrders.size() != displayOrders.size()) {
            throw ContentModuleException.badRequest("displayOrder must not contain duplicates");
        }
        if (displayOrders.stream().anyMatch(order -> order < 1)) {
            throw ContentModuleException.badRequest("displayOrder must be positive numbers");
        }

        if (mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndIsDeletedFalse(episodeId, uniqueOrders)) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }
        return displayOrders;
    }

    private void ensureImageMedia(Media media) {
        if (media.getMediaType() != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("displayOrder is only used for comic image media");
        }
    }

    private void ensureDisplayOrderAvailable(String episodeId, Integer displayOrder, String currentMediaId) {
        if (displayOrder == null || displayOrder < 1) {
            throw ContentModuleException.badRequest("displayOrder must be positive numbers");
        }

        boolean exists = currentMediaId == null
                ? mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndIsDeletedFalse(
                        episodeId,
                        List.of(displayOrder))
                : mediaRepository.existsByEpisode_EpisodeIdAndDisplayOrderInAndMediaIdNotInAndIsDeletedFalse(
                        episodeId,
                        List.of(displayOrder),
                        List.of(currentMediaId));
        if (exists) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }
    }

    private int nextDisplayOrder(String episodeId) {
        return mediaRepository.findMaxDisplayOrderByEpisodeId(episodeId) + 1;
    }

    private String normalizeStorageProvider(String storageProvider) {
        if (storageProvider == null || storageProvider.isBlank()) {
            return URL_STORAGE_PROVIDER;
        }
        return storageProvider.trim().toUpperCase(Locale.ROOT);
    }

    private MediaProvider resolveProvider(String storageProvider) {
        if (storageProvider == null || storageProvider.isBlank()) {
            return MediaProvider.URL;
        }
        try {
            return MediaProvider.valueOf(storageProvider.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ex) {
            return MediaProvider.URL;
        }
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private Episode lockActiveEpisode(String episodeId) {
        return episodeRepository.lockByEpisodeIdAndIsDeletedFalse(episodeId)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + episodeId));
    }
}
