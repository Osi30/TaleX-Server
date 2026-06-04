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
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.MediaService;
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

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {
    private static final String URL_STORAGE_PROVIDER = "URL";

    private final MediaRepository mediaRepository;
    private final EpisodeService episodeService;

    @Transactional
    @Override
    public MediaResponseDto createFromUrl(String episodeId, MediaMetadataRequestDto request) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        MediaType mediaType = resolveMediaType(episode, request != null ? request.getMediaType() : null);
        return createOneFromUrl(episode, request, mediaType, request != null ? request.getDisplayOrder() : null);
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
        if (media.getStatus() != MediaStatus.ACTIVE) {
            throw ContentModuleException.notFound("Public media not found: " + id);
        }
        episodeService.findPublicEntity(media.getEpisode().getEpisodeId());
        return toResponse(media);
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
                .findAllByEpisode_EpisodeIdAndStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
                        episodeId,
                        MediaStatus.ACTIVE)
                .stream()
                .map(this::toResponse)
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
        mediaRepository.save(media);
    }

    @Override
    public Media findActiveEntity(String id) {
        return mediaRepository.findByMediaIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Media not found: " + id));
    }

    @Override
    public MediaResponseDto toResponse(Media media) {
        return MediaResponseDto.builder()
                .mediaId(media.getMediaId())
                .episodeId(media.getEpisode().getEpisodeId())
                .mediaType(media.getMediaType())
                .mimeType(media.getMimeType())
                .fileUrl(media.getFileUrl())
                .cloudinaryPublicId(media.getCloudinaryPublicId())
                .storageProvider(media.getStorageProvider())
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
                .isDeleted(media.getIsDeleted())
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
        String checksum = checksum(fileUrl);
        rejectDuplicate(checksum, media.getMediaId());

        media.setFileUrl(fileUrl);
        media.setChecksum(checksum);
        media.setMimeType(resolveMimeType(fileUrl, mediaType));
        media.setFileSize(0L);
        media.setCloudinaryPublicId(null);
        media.setStorageProvider(URL_STORAGE_PROVIDER);
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
            boolean hasAnotherActiveVideo = mediaRepository
                    .findAllByEpisode_EpisodeIdAndStatusAndIsDeletedFalseOrderByDisplayOrderAsc(
                            episode.getEpisodeId(),
                            MediaStatus.ACTIVE)
                    .stream()
                    .anyMatch(media -> currentMediaId == null || !media.getMediaId().equals(currentMediaId));
            if (hasAnotherActiveVideo) {
                throw ContentModuleException.conflict("Video episode already has an active video media");
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

    private String resolveMimeType(String fileUrl, MediaType mediaType) {
        String lowerUrl = fileUrl.toLowerCase(Locale.ROOT);
        int queryStart = lowerUrl.indexOf('?');
        if (queryStart >= 0) {
            lowerUrl = lowerUrl.substring(0, queryStart);
        }

        if (mediaType == MediaType.VIDEO) {
            if (lowerUrl.endsWith(".mp4")) {
                return "video/mp4";
            }
            if (lowerUrl.endsWith(".webm")) {
                return "video/webm";
            }
            if (lowerUrl.endsWith(".mov")) {
                return "video/quicktime";
            }
            if (lowerUrl.endsWith(".m3u8")) {
                return "application/vnd.apple.mpegurl";
            }
            return "video/url";
        }

        if (lowerUrl.endsWith(".jpg") || lowerUrl.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (lowerUrl.endsWith(".png")) {
            return "image/png";
        }
        if (lowerUrl.endsWith(".webp")) {
            return "image/webp";
        }
        if (lowerUrl.endsWith(".gif")) {
            return "image/gif";
        }
        return "image/url";
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

    private Integer resolveDisplayOrder(String episodeId, MediaType mediaType, Integer requestedDisplayOrder) {
        if (mediaType == MediaType.VIDEO) {
            return null;
        }
        return requestedDisplayOrder != null ? requestedDisplayOrder : nextDisplayOrder(episodeId);
    }

    private List<Integer> resolveComicDisplayOrders(String episodeId, List<MediaComicPageRequestDto> pages) {
        boolean hasProvidedOrder = pages.stream().anyMatch(page -> page.getDisplayOrder() != null);
        if (!hasProvidedOrder) {
            int nextOrder = nextDisplayOrder(episodeId);
            List<Integer> generatedOrders = new ArrayList<>();
            for (int i = 0; i < pages.size(); i++) {
                generatedOrders.add(nextOrder + i);
            }
            return generatedOrders;
        }

        if (pages.stream().anyMatch(page -> page.getDisplayOrder() == null)) {
            throw ContentModuleException.badRequest("displayOrder must be provided for every page, or omitted for every page");
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
}
