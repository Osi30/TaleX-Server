package com.talex.server.services.impls;

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
import com.talex.server.policies.FilePolicy;
import com.talex.server.records.CloudinaryUploadResult;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.CloudinaryStorageService;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.MediaService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class MediaServiceImpl implements MediaService {
    private static final String CLOUDINARY_PROVIDER = "CLOUDINARY";

    private final MediaRepository mediaRepository;
    private final EpisodeService episodeService;
    private final CloudinaryStorageService cloudinaryStorageService;

    @Transactional(noRollbackFor = ContentModuleException.class)
    @Override
    public MediaResponseDto upload(String episodeId, MultipartFile file, MediaMetadataRequestDto request) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        return uploadOne(episode, file, request, request.getMediaType(), request.getDisplayOrder());
    }

    @Transactional(noRollbackFor = ContentModuleException.class)
    @Override
    public List<MediaResponseDto> uploadComicPages(
            String episodeId,
            List<MultipartFile> files,
            List<Integer> displayOrders,
            String actorId) {

        Episode episode = episodeService.findActiveEntity(episodeId);
        if (episode.getContentType() != ContentType.COMIC) {
            throw ContentModuleException.badRequest("Batch media upload is only supported for comic episodes");
        }
        if (files == null || files.isEmpty()) {
            throw ContentModuleException.badRequest("At least one image file is required");
        }

        List<Integer> resolvedOrders = resolveComicDisplayOrders(episodeId, files.size(), displayOrders);
        List<MediaResponseDto> responses = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            MediaMetadataRequestDto metadata = new MediaMetadataRequestDto();
            metadata.setMediaType(MediaType.IMAGE);
            metadata.setDisplayOrder(resolvedOrders.get(i));
            metadata.setActorId(actorId);
            responses.add(uploadOne(episode, files.get(i), metadata, MediaType.IMAGE, resolvedOrders.get(i)));
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

    @Transactional(noRollbackFor = ContentModuleException.class)
    @Override
    public MediaResponseDto replaceFile(String id, MultipartFile file, MediaMetadataRequestDto request) {
        Media media = findActiveEntity(id);
        MediaType mediaType = resolveMediaType(file, request.getMediaType() != null ? request.getMediaType() : media.getMediaType());
        if (media.getMediaType() != mediaType) {
            throw ContentModuleException.badRequest("Replacement file type must match existing media type");
        }
        validateMediaForEpisode(media.getEpisode(), mediaType, media.getMediaId());
        validateFile(file, mediaType);

        String checksum = checksum(file);
        rejectDuplicate(checksum, media.getMediaId());

        media.setStatus(MediaStatus.PROCESSING);
        media.markUpdatedBy(request.getActorId());
        mediaRepository.save(media);

        try {
            CloudinaryUploadResult uploadResult = cloudinaryStorageService.upload(file, mediaType);
            applyCloudinaryUpload(media, uploadResult, file);
            media.setChecksum(checksum);
            applyMetadata(media, request, mediaType);
            media.setStatus(MediaStatus.ACTIVE);
            media.markUpdatedBy(request.getActorId());
            return toResponse(mediaRepository.save(media));
        } catch (ContentModuleException exception) {
            media.setStatus(MediaStatus.FAILED);
            media.markUpdatedBy(request.getActorId());
            mediaRepository.save(media);
            throw exception;
        }
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

    private MediaResponseDto uploadOne(
            Episode episode,
            MultipartFile file,
            MediaMetadataRequestDto request,
            MediaType requestedType,
            Integer requestedDisplayOrder) {

        MediaType mediaType = resolveMediaType(file, requestedType);
        validateMediaForEpisode(episode, mediaType, null);
        validateFile(file, mediaType);

        String checksum = checksum(file);
        rejectDuplicate(checksum, null);

        Media media = new Media();
        media.setEpisode(episode);
        media.setMediaType(mediaType);
        media.setMimeType(file.getContentType());
        media.setFileSize(file.getSize());
        media.setChecksum(checksum);
        media.setFileUrl("");
        media.setStatus(MediaStatus.PROCESSING);
        media.setDisplayOrder(resolveDisplayOrder(episode.getEpisodeId(), mediaType, requestedDisplayOrder));
        applyMetadata(media, request, mediaType);
        media.markCreatedBy(request.getActorId());
        media = mediaRepository.save(media);

        try {
            CloudinaryUploadResult uploadResult = cloudinaryStorageService.upload(file, mediaType);
            applyCloudinaryUpload(media, uploadResult, file);
            media.setStatus(MediaStatus.ACTIVE);
            media.markUpdatedBy(request.getActorId());
            return toResponse(mediaRepository.save(media));
        } catch (ContentModuleException exception) {
            media.setStatus(MediaStatus.FAILED);
            media.markUpdatedBy(request.getActorId());
            mediaRepository.save(media);
            throw exception;
        }
    }

    private void applyCloudinaryUpload(Media media, CloudinaryUploadResult uploadResult, MultipartFile file) {
        media.setFileUrl(uploadResult.secureUrl());
        media.setCloudinaryPublicId(uploadResult.publicId());
        media.setStorageProvider(CLOUDINARY_PROVIDER);
        media.setMimeType(file.getContentType());
        media.setFileSize(uploadResult.bytes() != null ? uploadResult.bytes() : file.getSize());

        if (media.getWidth() == null) {
            media.setWidth(uploadResult.width());
        }
        if (media.getHeight() == null) {
            media.setHeight(uploadResult.height());
        }
        if (media.getDuration() == null) {
            media.setDuration(uploadResult.duration());
        }
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

    private MediaType resolveMediaType(MultipartFile file, MediaType requestedType) {
        if (requestedType != null) {
            return requestedType;
        }

        String contentType = file.getContentType();
        if (contentType != null && contentType.startsWith("video/")) {
            return MediaType.VIDEO;
        }
        if (contentType != null && contentType.startsWith("image/")) {
            return MediaType.IMAGE;
        }
        throw ContentModuleException.badRequest("Cannot detect media type from file content type");
    }

    private void validateMediaForEpisode(Episode episode, MediaType mediaType, String currentMediaId) {
        if (episode.getContentType() == ContentType.VIDEO && mediaType != MediaType.VIDEO) {
            throw ContentModuleException.badRequest("Video episode only accepts video media");
        }
        if (episode.getContentType() == ContentType.COMIC && mediaType != MediaType.IMAGE) {
            throw ContentModuleException.badRequest("Comic episode only accepts image media");
        }
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

    private void validateFile(MultipartFile file, MediaType mediaType) {
        if (file == null || file.isEmpty()) {
            throw ContentModuleException.badRequest("File is required");
        }

        FilePolicy policy = mediaType == MediaType.VIDEO ? FilePolicy.CONTENT_VIDEO : FilePolicy.CONTENT_IMAGE;
        if (file.getSize() > policy.getMaxSizeBytes()) {
            throw ContentModuleException.badRequest("File exceeds max size: " + policy.getMaxSizeLabel());
        }

        String contentType = file.getContentType();
        boolean allowed = contentType != null && Arrays.asList(policy.getAllowedContentTypes()).contains(contentType);
        if (!allowed) {
            throw ContentModuleException.badRequest("Invalid file type. Allowed: " + policy.getAllowedExtensionsLabel());
        }
    }

    private void rejectDuplicate(String checksum, String currentMediaId) {
        mediaRepository.findFirstByChecksumAndIsDeletedFalse(checksum)
                .filter(existing -> currentMediaId == null || !existing.getMediaId().equals(currentMediaId))
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Duplicate media file detected by checksum");
                });
    }

    private String checksum(MultipartFile file) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            try (InputStream inputStream = file.getInputStream();
                 DigestInputStream digestInputStream = new DigestInputStream(inputStream, digest)) {
                byte[] buffer = new byte[8192];
                while (digestInputStream.read(buffer) != -1) {
                    // DigestInputStream updates the digest while the stream is consumed.
                }
            }
            return HexFormat.of().formatHex(digest.digest());
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ContentModuleException(
                    4301,
                    org.springframework.http.HttpStatus.BAD_REQUEST,
                    "Cannot generate file checksum",
                    exception);
        }
    }

    private Integer resolveDisplayOrder(String episodeId, MediaType mediaType, Integer requestedDisplayOrder) {
        if (mediaType == MediaType.VIDEO) {
            return null;
        }
        return requestedDisplayOrder != null ? requestedDisplayOrder : nextDisplayOrder(episodeId);
    }

    private List<Integer> resolveComicDisplayOrders(String episodeId, int fileCount, List<Integer> displayOrders) {
        if (displayOrders == null || displayOrders.isEmpty()) {
            throw ContentModuleException.badRequest("displayOrders is required for comic page upload");
        }

        if (displayOrders.size() != fileCount) {
            throw ContentModuleException.badRequest("displayOrders size must match files size");
        }

        Set<Integer> uniqueOrders = new HashSet<>(displayOrders);
        if (uniqueOrders.size() != displayOrders.size()) {
            throw ContentModuleException.badRequest("displayOrders must not contain duplicates");
        }
        if (displayOrders.stream().anyMatch(order -> order == null || order < 1)) {
            throw ContentModuleException.badRequest("displayOrders must be positive numbers");
        }

        Set<Integer> existingOrders = mediaRepository
                .findAllByEpisode_EpisodeIdAndIsDeletedFalseOrderByDisplayOrderAsc(episodeId)
                .stream()
                .map(Media::getDisplayOrder)
                .filter(order -> order != null)
                .collect(java.util.stream.Collectors.toSet());
        if (displayOrders.stream().anyMatch(existingOrders::contains)) {
            throw ContentModuleException.conflict("displayOrders already exist in this episode");
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
