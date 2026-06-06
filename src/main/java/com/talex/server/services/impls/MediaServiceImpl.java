package com.talex.server.services.impls;

import com.talex.server.dtos.requests.MediaComicPageRequestDto;
import com.talex.server.dtos.requests.MediaComicPagesRequestDto;
import com.talex.server.dtos.requests.MediaMetadataRequestDto;
import com.talex.server.dtos.requests.MediaReorderRequestDto;
import com.talex.server.dtos.requests.MediaStatusRequestDto;
import com.talex.server.dtos.requests.MediaUpdateRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.entities.Episode;
import com.talex.server.entities.Media;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.MediaPlaybackPolicy;
import com.talex.server.enums.MediaProtectionType;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.MediaPlaybackSecurityService;
import com.talex.server.services.MediaService;
import com.talex.server.services.media.MediaProviderService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {
    private static final Pattern SHA256_PATTERN = Pattern.compile("^[a-fA-F0-9]{64}$");
    private static final String URL_STORAGE_PROVIDER = "URL";
    private static final List<MediaStatus> PUBLIC_READY_STATUSES = List.of(MediaStatus.ACTIVE, MediaStatus.HLS_READY);
    private static final List<MediaStatus> VIDEO_REPLACEMENT_BLOCKING_STATUSES = List.of(
            MediaStatus.ACTIVE,
            MediaStatus.HLS_READY,
            MediaStatus.HLS_PROCESSING);

    private final MediaRepository mediaRepository;
    private final EpisodeService episodeService;
    private final MediaProviderService mediaProviderService;
    private final MediaPlaybackSecurityService playbackSecurityService;

    @Transactional
    @Override
    public MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        if (request == null) {
            throw ContentModuleException.badRequest("Media URL request is required");
        }
        MediaType mediaType = resolveMediaType(episode, request.getMediaType());
        return createOneFromUrl(episode, request, mediaType, request.getDisplayOrder());
    }

    @Transactional
    @Override
    public List<MediaResponseDto> createComicPagesFromUrls(String episodeId, MediaComicPagesRequestDto request) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Batch media URL creation is only supported for comic episodes");
        }
        if (request == null || request.getPages() == null || request.getPages().isEmpty()) {
            throw ContentModuleException.badRequest("At least one comic page URL is required");
        }
        if (request.getPages().stream().anyMatch(page -> page == null)) {
            throw ContentModuleException.badRequest("Comic page item must not be null");
        }

        List<Integer> resolvedOrders = resolveComicDisplayOrders(episodeId, request.getPages());
        List<MediaResponseDto> responses = new ArrayList<>();
        for (int i = 0; i < request.getPages().size(); i++) {
            MediaComicPageRequestDto page = request.getPages().get(i);
            MediaMetadataRequestDto metadata = new MediaMetadataRequestDto();
            metadata.setFileUrl(page.getFileUrl());
            metadata.setMediaType(MediaType.IMAGE);
            metadata.setDisplayOrder(resolvedOrders.get(i));
            metadata.setMimeType(page.getMimeType());
            metadata.setFileSize(page.getFileSize());
            metadata.setChecksum(page.getChecksum());
            metadata.setExternalPublicId(page.getExternalPublicId());
            metadata.setStorageProvider(page.getStorageProvider());
            metadata.setWidth(page.getWidth());
            metadata.setHeight(page.getHeight());
            metadata.setResolution(page.getResolution());
            metadata.setActorId(request.getActorId());
            responses.add(createOneFromUrl(episode, metadata, MediaType.IMAGE, resolvedOrders.get(i)));
        }

        return responses;
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public MediaResponseDto getPublicById(String id) {
        Media media = findActiveEntity(id);
        if (!PUBLIC_READY_STATUSES.contains(media.getStatus())) {
            throw ContentModuleException.notFound("Public media not found: " + id);
        }
        episodeService.findPublicEntity(media.getEpisode().getEpisodeId());
        return toPublicResponse(media);
    }

    @Transactional(readOnly = true)
    @Override
    public List<MediaResponseDto> listByEpisode(String episodeId) {
        episodeService.findActiveEntity(episodeId);
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
                .findAllByEpisode_EpisodeIdAndStatusInAndIsDeletedFalseOrderByDisplayOrderAsc(
                        episodeId,
                        PUBLIC_READY_STATUSES)
                .stream()
                .map(this::toPublicResponse)
                .sorted(Comparator.comparing(MediaResponseDto::getDisplayOrder, Comparator.nullsLast(Integer::compareTo)))
                .toList();
    }

    @Transactional
    @Override
    public MediaResponseDto update(String id, MediaUpdateRequestDto request) {
        Media media = findActiveEntity(id);
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
            media.setDisplayOrder(request.getDisplayOrder());
        }
        if (request.getStatus() != null) {
            media.setStatus(request.getStatus());
        }
        media.markUpdatedBy(request.getActorId());
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto replaceUrl(String id, MediaMetadataRequestDto request) {
        Media media = findActiveEntity(id);
        MediaType mediaType = resolveMediaType(
                media.getEpisode(),
                request != null && request.getMediaType() != null ? request.getMediaType() : media.getMediaType());
        if (media.getMediaType() != mediaType) {
            throw ContentModuleException.badRequest("Replacement URL type must match existing media type");
        }

        validateMediaForEpisode(media.getEpisode(), mediaType, media.getMediaId());
        applyUrl(media, request, mediaType);
        media.markUpdatedBy(request.getActorId());
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public List<MediaResponseDto> reorder(String episodeId, MediaReorderRequestDto request) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Only comic episode media can be reordered");
        }

        for (var item : request.getItems()) {
            Media media = findActiveEntity(item.getMediaId());
            if (!media.getEpisode().getEpisodeId().equals(episodeId)) {
                throw ContentModuleException.badRequest("Media does not belong to episode: " + item.getMediaId());
            }
            ensureImageMedia(media);
            media.setDisplayOrder(item.getDisplayOrder());
            media.markUpdatedBy(request.getActorId());
            mediaRepository.save(media);
        }

        return listByEpisode(episodeId);
    }

    @Transactional
    @Override
    public MediaResponseDto hide(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setStatus(MediaStatus.HIDDEN);
        media.markUpdatedBy(actorId);
        playbackSecurityService.revokeActiveSessions(media);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto unhide(String id, String actorId) {
        Media media = findActiveEntity(id);
        media.setStatus(MediaStatus.ACTIVE);
        media.markUpdatedBy(actorId);
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public MediaResponseDto updateProcessingStatus(String id, MediaStatusRequestDto request) {
        Media media = findActiveEntity(id);
        if (request.getStatus() == MediaStatus.DELETED) {
            media.setStatus(MediaStatus.DELETED);
            media.softDelete(request.getActorId());
            playbackSecurityService.revokeActiveSessions(media);
        } else {
            media.setStatus(request.getStatus());
            media.markUpdatedBy(request.getActorId());
        }
        return toResponse(mediaRepository.save(media));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Media media = findActiveEntity(id);
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
    public MediaResponseDto toResponse(Media media) {
        return baseResponse(media).build();
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
                .createdAt(media.getCreatedAt())
                .updatedAt(media.getUpdatedAt())
                .deletedAt(media.getDeletedAt())
                .createdBy(media.getCreatedBy())
                .updatedBy(media.getUpdatedBy())
                .deletedBy(media.getDeletedBy())
                .isDeleted(media.getIsDeleted());
    }

    private MediaResponseDto createOneFromUrl(
            Episode episode,
            MediaMetadataRequestDto request,
            MediaType mediaType,
            Integer requestedDisplayOrder) {

        validateMediaForEpisode(episode, mediaType, null);

        Media media = new Media();
        media.setEpisode(episode);
        media.setMediaType(mediaType);
        media.setDisplayOrder(resolveDisplayOrder(episode.getEpisodeId(), mediaType, requestedDisplayOrder));
        applyUrl(media, request, mediaType);
        media.markCreatedBy(request.getActorId());

        return toResponse(mediaRepository.save(media));
    }

    private void applyUrl(Media media, MediaMetadataRequestDto request, MediaType mediaType) {
        if (request == null) {
            throw ContentModuleException.badRequest("Media URL request is required");
        }

        String fileUrl = normalizeFileUrl(request.getFileUrl());
        String checksum = normalizeChecksum(request.getChecksum(), fileUrl);
        rejectDuplicate(checksum, media.getMediaId());

        media.setFileUrl(fileUrl);
        media.setOriginalUrl(firstNonBlank(request.getOriginalUrl(), fileUrl));
        media.setPlaybackUrl(firstNonBlank(request.getPlaybackUrl(), request.getHlsUrl(), fileUrl));
        media.setHlsUrl(request.getHlsUrl());
        media.setThumbnailUrl(request.getThumbnailUrl());
        media.setPreviewUrl(request.getPreviewUrl());
        media.setChecksum(checksum);
        media.setMimeType(validateMimeType(request.getMimeType(), mediaType));
        media.setFileSize(validateFileSize(request.getFileSize()));
        media.setExternalPublicId(blankToNull(request.getExternalPublicId()));
        media.setStorageProvider(normalizeStorageProvider(request.getStorageProvider()));
        media.setProvider(request.getProvider() != null ? request.getProvider() : resolveProvider(request.getStorageProvider()));
        media.setProviderAssetId(blankToNull(request.getProviderAssetId()));
        media.setProviderPublicId(firstNonBlank(request.getProviderPublicId(), request.getExternalPublicId()));
        media.setProviderDeliveryType(blankToNull(request.getProviderDeliveryType()));
        media.setFormat(blankToNull(request.getFormat()));
        media.setProtectionType(request.getProtectionType() != null ? request.getProtectionType() : MediaProtectionType.NONE);
        media.setPlaybackPolicy(request.getPlaybackPolicy() != null ? request.getPlaybackPolicy() : MediaPlaybackPolicy.PUBLIC);
        media.setStatus(MediaStatus.ACTIVE);
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
            boolean hasAnotherReadyVideo = mediaRepository
                    .findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                            episode.getEpisodeId(),
                            MediaType.VIDEO,
                            VIDEO_REPLACEMENT_BLOCKING_STATUSES)
                    .stream()
                    .anyMatch(media -> currentMediaId == null || !media.getMediaId().equals(currentMediaId));
            if (hasAnotherReadyVideo) {
                throw ContentModuleException.conflict("Video episode already has a video media or HLS processing is still running");
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
                .filter(existing -> currentMediaId == null || !existing.getMediaId().equals(currentMediaId))
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
        return requestedDisplayOrder != null ? requestedDisplayOrder : nextDisplayOrder(episodeId);
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

        Set<Integer> existingOrders = mediaRepository
                .findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId)
                .stream()
                .map(Media::getDisplayOrder)
                .filter(order -> order != null)
                .collect(java.util.stream.Collectors.toSet());
        if (displayOrders.stream().anyMatch(existingOrders::contains)) {
            throw ContentModuleException.conflict("displayOrder already exists in this episode");
        }
        return displayOrders;
    }

    private void ensureImageMedia(Media media) {
        if (media.getMediaType() != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("displayOrder is only used for comic image media");
        }
    }

    private int nextDisplayOrder(String episodeId) {
        return mediaRepository.findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId)
                .stream()
                .map(Media::getDisplayOrder)
                .filter(order -> order != null)
                .max(Integer::compareTo)
                .orElse(0) + 1;
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
}
