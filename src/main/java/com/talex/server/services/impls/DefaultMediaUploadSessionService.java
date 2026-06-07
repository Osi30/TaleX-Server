package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.MediaUploadFailRequestDto;
import com.talex.server.dtos.requests.MediaUploadProgressRequestDto;
import com.talex.server.dtos.requests.VideoUploadSessionRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.dtos.responses.MediaUploadSessionResponseDto;
import com.talex.server.dtos.responses.VideoUploadSessionResponseDto;
import com.talex.server.entities.Episode;
import com.talex.server.entities.Media;
import com.talex.server.entities.MediaUploadSession;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.MediaPlaybackPolicy;
import com.talex.server.enums.MediaProtectionType;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import com.talex.server.enums.MediaUploadSessionStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.repositories.MediaUploadSessionRepository;
import com.talex.server.services.EpisodeService;
import com.talex.server.services.MediaService;
import com.talex.server.services.MediaUploadSessionService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.SignedUploadParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMediaUploadSessionService implements MediaUploadSessionService {
    private static final long MB = 1024L * 1024L;
    private static final List<MediaStatus> VIDEO_REPLACEMENT_CHECK_STATUSES = List.of(
            MediaStatus.PROCESSING,
            MediaStatus.HLS_PROCESSING,
            MediaStatus.HLS_READY,
            MediaStatus.ACTIVE);

    private final MediaUploadSessionRepository uploadSessionRepository;
    private final MediaRepository mediaRepository;
    private final EpisodeService episodeService;
    private final MediaService mediaService;
    private final MediaProviderService mediaProviderService;
    private final MediaProperties mediaProperties;

    @Transactional
    @Override
    public VideoUploadSessionResponseDto createVideoUploadSession(String episodeId, VideoUploadSessionRequestDto request) {
        Episode episode = episodeService.findActiveEntity(episodeId);
        validateVideoEpisode(episode);
        validateVideoRequest(request);

        reconcileExistingVideoMedia(episodeId, request.getActorId());

        MediaProtectionType protectionType = request.getProtectionType() != null
                ? request.getProtectionType()
                : mediaProperties.getDefaultVideoProtectionType();
        MediaPlaybackPolicy playbackPolicy = protectionType == MediaProtectionType.NONE
                ? MediaPlaybackPolicy.PUBLIC
                : MediaPlaybackPolicy.SIGNED;
        long chunkSize = resolveChunkSize(request.getChunkSize());

        Media media = new Media();
        media.setEpisode(episode);
        media.setMediaType(MediaType.VIDEO);
        media.setMimeType(normalizeMimeType(request.getMimeType()));
        media.setFileSize(request.getFileSize());
        media.setFileUrl("pending://video-upload/" + UUID.randomUUID());
        media.setChecksum("pending:" + UUID.randomUUID());
        media.setProvider(MediaProvider.CLOUDINARY);
        media.setProviderDeliveryType(mediaProperties.getCloudinary().getProviderDeliveryType());
        media.setProtectionType(protectionType);
        media.setPlaybackPolicy(playbackPolicy);
        media.setTokenTtlSeconds(mediaProperties.getSignedPlaybackTtlSeconds().intValue());
        media.setStatus(MediaStatus.PROCESSING);
        media.markCreatedBy(request.getActorId());
        media = mediaRepository.save(media);

        String providerPublicId = mediaProviderService.buildVideoPublicId(episodeId, media.getMediaId());
        media.setProviderPublicId(providerPublicId);
        media.setStorageProvider(MediaProvider.CLOUDINARY.name());
        media.setExternalPublicId(providerPublicId);
        mediaRepository.save(media);

        MediaUploadSession session = new MediaUploadSession();
        session.setUploadSessionId(UUID.randomUUID().toString());
        session.setMedia(media);
        session.setEpisode(episode);
        session.setCreatorId(blankToNull(request.getCreatorId()));
        session.setProvider(MediaProvider.CLOUDINARY);
        session.setProviderPublicId(providerPublicId);
        session.setProviderDeliveryType(media.getProviderDeliveryType());
        session.setUploadUniqueId(UUID.randomUUID().toString());
        session.setFileName(request.getFileName().trim());
        session.setFileSize(request.getFileSize());
        session.setMimeType(normalizeMimeType(request.getMimeType()));
        session.setChunkSize(chunkSize);
        session.setUploadedBytes(0L);
        session.setTotalChunks((int) Math.ceil((double) request.getFileSize() / (double) chunkSize));
        session.setStatus(MediaUploadSessionStatus.INITIATED);
        session.setExpiredAt(LocalDateTime.now().plusHours(24));
        session.markCreatedBy(request.getActorId());
        session = uploadSessionRepository.save(session);

        SignedUploadParams signedUpload = mediaProviderService.createSignedUploadParams(
                providerPublicId,
                session.getProviderDeliveryType());

        log.info("Video upload session created. uploadSessionId={} mediaId={} episodeId={} fileSize={}",
                session.getUploadSessionId(), media.getMediaId(), episodeId, request.getFileSize());

        return toSignedResponse(session, signedUpload);
    }

    @Transactional(readOnly = true)
    @Override
    public MediaUploadSessionResponseDto getSession(String uploadSessionId) {
        return toResponse(findSession(uploadSessionId));
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto updateProgress(String uploadSessionId, MediaUploadProgressRequestDto request) {
        MediaUploadSession session = findSession(uploadSessionId);
        if (request.getUploadedBytes() > session.getFileSize()) {
            throw ContentModuleException.badRequest("uploadedBytes cannot exceed fileSize");
        }
        if (session.getStatus() == MediaUploadSessionStatus.COMPLETED
                || session.getStatus() == MediaUploadSessionStatus.CANCELLED
                || session.getStatus() == MediaUploadSessionStatus.EXPIRED) {
            throw ContentModuleException.badRequest("Upload session cannot accept progress in status " + session.getStatus());
        }

        session.setUploadedBytes(request.getUploadedBytes());
        session.setLastUploadedChunkIndex(request.getLastUploadedChunkIndex());
        session.setStatus(request.getStatus() == null ? MediaUploadSessionStatus.UPLOADING : request.getStatus());
        session.markUpdatedBy(request.getActorId());
        log.info("Video upload progress updated. uploadSessionId={} uploadedBytes={}",
                uploadSessionId, request.getUploadedBytes());
        return toResponse(uploadSessionRepository.save(session));
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto pause(String uploadSessionId, String actorId) {
        MediaUploadSession session = findSession(uploadSessionId);
        session.setStatus(MediaUploadSessionStatus.PAUSED);
        session.markUpdatedBy(actorId);
        log.info("Video upload paused. uploadSessionId={}", uploadSessionId);
        return toResponse(uploadSessionRepository.save(session));
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto fail(String uploadSessionId, MediaUploadFailRequestDto request) {
        MediaUploadSession session = findSession(uploadSessionId);
        session.setStatus(MediaUploadSessionStatus.FAILED);
        session.setErrorMessage(request == null ? null : blankToNull(request.getErrorMessage()));
        session.markUpdatedBy(request == null ? null : request.getActorId());
        if (session.getMedia() != null) {
            session.getMedia().setStatus(MediaStatus.FAILED);
            session.getMedia().setErrorMessage(session.getErrorMessage());
            mediaRepository.save(session.getMedia());
        }
        log.warn("Video upload failed. uploadSessionId={} error={}", uploadSessionId, session.getErrorMessage());
        return toResponse(uploadSessionRepository.save(session));
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto cancel(String uploadSessionId, String actorId) {
        MediaUploadSession session = findSession(uploadSessionId);
        session.setStatus(MediaUploadSessionStatus.CANCELLED);
        session.markUpdatedBy(actorId);
        if (session.getMedia() != null) {
            Media media = session.getMedia();
            media.setStatus(MediaStatus.DELETED);
            media.softDelete(actorId);
            mediaProviderService.deleteAsset(media);
            mediaRepository.save(media);
        }
        log.info("Video upload cancelled. uploadSessionId={}", uploadSessionId);
        return toResponse(uploadSessionRepository.save(session));
    }

    @Transactional
    @Override
    public MediaResponseDto complete(String uploadSessionId, MediaUploadCompleteRequestDto request) {
        MediaUploadSession session = findSession(uploadSessionId);
        if (session.getStatus() == MediaUploadSessionStatus.CANCELLED
                || session.getStatus() == MediaUploadSessionStatus.EXPIRED) {
            throw ContentModuleException.badRequest("Upload session cannot complete in status " + session.getStatus());
        }
        if (!session.getProviderPublicId().equals(request.getPublicId())) {
            throw ContentModuleException.badRequest("Cloudinary publicId does not match upload session");
        }
        if (request.getBytes() > session.getFileSize()) {
            throw ContentModuleException.badRequest("Completed byte size cannot exceed declared fileSize");
        }

        Media media = session.getMedia();
        if (media == null) {
            throw ContentModuleException.notFound("Reserved media not found for upload session");
        }

        mediaProviderService.applyCompletedUpload(media, session, request);
        media.setProtectionType(media.getProtectionType() == null
                ? mediaProperties.getDefaultVideoProtectionType()
                : media.getProtectionType());
        media.setPlaybackPolicy(media.getProtectionType() == MediaProtectionType.NONE
                ? MediaPlaybackPolicy.PUBLIC
                : MediaPlaybackPolicy.SIGNED);
        media.markUpdatedBy(request.getActorId());
        media = mediaRepository.save(media);
        log.info("HLS_PROCESSING_STARTED upload completed; waiting for Cloudinary eager webhook. mediaId={} providerPublicId={}",
                media.getMediaId(), media.getProviderPublicId());

        session.setUploadedBytes(request.getBytes());
        session.setLastUploadedChunkIndex(session.getTotalChunks() == null ? null : session.getTotalChunks() - 1);
        session.setStatus(MediaUploadSessionStatus.COMPLETED);
        session.setErrorMessage(null);
        session.markUpdatedBy(request.getActorId());
        uploadSessionRepository.save(session);

        log.info("Video upload completed. uploadSessionId={} mediaId={} providerPublicId={}",
                uploadSessionId, media.getMediaId(), media.getProviderPublicId());
        return mediaService.toResponse(media);
    }

    private void reconcileExistingVideoMedia(String episodeId, String actorId) {
        var existingVideos = mediaRepository
                .findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                        episodeId,
                        MediaType.VIDEO,
                        VIDEO_REPLACEMENT_CHECK_STATUSES);

        for (Media media : existingVideos) {
            if (media.getStatus() == MediaStatus.ACTIVE
                    || media.getStatus() == MediaStatus.HLS_READY
                    || media.getStatus() == MediaStatus.HLS_PROCESSING) {
                throw ContentModuleException.conflict("Video episode already has a video media or HLS processing is still running. Delete it before uploading a replacement.");
            }

            var sessions = uploadSessionRepository.findAllByMedia_MediaIdAndIsDeletedFalse(media.getMediaId());
            sessions.stream()
                    .filter(session -> session.getStatus() != MediaUploadSessionStatus.COMPLETED)
                    .forEach(session -> {
                        session.setStatus(MediaUploadSessionStatus.CANCELLED);
                        session.setErrorMessage("Superseded by a new upload session");
                        session.markUpdatedBy(actorId);
                        uploadSessionRepository.save(session);
                    });

            media.setStatus(MediaStatus.DELETED);
            media.setErrorMessage("Superseded by a new upload session");
            media.softDelete(actorId);
            mediaRepository.save(media);
            log.info("Cancelled stale processing video before creating a new upload session. mediaId={} episodeId={}",
                    media.getMediaId(), episodeId);
        }
    }
    private MediaUploadSession findSession(String uploadSessionId) {
        return uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse(uploadSessionId)
                .orElseThrow(() -> ContentModuleException.notFound("Upload session not found: " + uploadSessionId));
    }

    private void validateVideoEpisode(Episode episode) {
        if (episode.getContentType() != ContentType.VIDEO) {
            throw ContentModuleException.badRequest("Video upload session is only supported for VIDEO episodes");
        }
    }

    private void validateVideoRequest(VideoUploadSessionRequestDto request) {
        if (request == null) {
            throw ContentModuleException.badRequest("Video upload session request is required");
        }
        String mimeType = normalizeMimeType(request.getMimeType());
        boolean allowed = mediaProperties.getAllowedVideoMimeTypes().stream()
                .map(value -> value.toLowerCase(Locale.ROOT))
                .anyMatch(mimeType::equals);
        if (!allowed) {
            throw ContentModuleException.badRequest("Unsupported video mimeType: " + request.getMimeType());
        }
        long maxBytes = mediaProperties.getMaxVideoSizeMb() * MB;
        if (request.getFileSize() > maxBytes) {
            throw ContentModuleException.badRequest("Video file exceeds max size of " + mediaProperties.getMaxVideoSizeMb() + "MB");
        }
        resolveChunkSize(request.getChunkSize());
    }

    private long resolveChunkSize(Long requestedChunkSize) {
        long chunkSize = requestedChunkSize == null
                ? mediaProperties.getVideoUploadChunkSizeMb() * MB
                : requestedChunkSize;
        long minChunkSize = mediaProperties.getMinVideoUploadChunkSizeMb() * MB;
        if (chunkSize < minChunkSize) {
            throw ContentModuleException.badRequest("chunkSize must be at least " + mediaProperties.getMinVideoUploadChunkSizeMb() + "MB");
        }
        return chunkSize;
    }

    private String normalizeMimeType(String mimeType) {
        if (mimeType == null || mimeType.isBlank()) {
            throw ContentModuleException.badRequest("mimeType is required");
        }
        return mimeType.trim().toLowerCase(Locale.ROOT);
    }

    private VideoUploadSessionResponseDto toSignedResponse(MediaUploadSession session, SignedUploadParams signedUpload) {
        return VideoUploadSessionResponseDto.builder()
                .uploadSessionId(session.getUploadSessionId())
                .mediaId(session.getMedia().getMediaId())
                .episodeId(session.getEpisode().getEpisodeId())
                .provider(session.getProvider())
                .cloudName(signedUpload.getCloudName())
                .apiKey(signedUpload.getApiKey())
                .timestamp(signedUpload.getTimestamp())
                .signature(signedUpload.getSignature())
                .publicId(session.getProviderPublicId())
                .resourceType(signedUpload.getResourceType())
                .uploadUrl(signedUpload.getUploadUrl())
                .uploadUniqueId(session.getUploadUniqueId())
                .chunkSize(session.getChunkSize())
                .fileSize(session.getFileSize())
                .fileName(session.getFileName())
                .mimeType(session.getMimeType())
                .providerDeliveryType(session.getProviderDeliveryType())
                .uploadParams(signedUpload.getUploadParams())
                .uploadedBytes(session.getUploadedBytes())
                .lastUploadedChunkIndex(session.getLastUploadedChunkIndex())
                .status(session.getStatus())
                .expiredAt(session.getExpiredAt())
                .build();
    }

    private MediaUploadSessionResponseDto toResponse(MediaUploadSession session) {
        return MediaUploadSessionResponseDto.builder()
                .uploadSessionId(session.getUploadSessionId())
                .mediaId(session.getMedia() == null ? null : session.getMedia().getMediaId())
                .episodeId(session.getEpisode().getEpisodeId())
                .creatorId(session.getCreatorId())
                .provider(session.getProvider())
                .providerPublicId(session.getProviderPublicId())
                .providerDeliveryType(session.getProviderDeliveryType())
                .uploadUniqueId(session.getUploadUniqueId())
                .fileName(session.getFileName())
                .fileSize(session.getFileSize())
                .mimeType(session.getMimeType())
                .chunkSize(session.getChunkSize())
                .uploadedBytes(session.getUploadedBytes())
                .totalChunks(session.getTotalChunks())
                .lastUploadedChunkIndex(session.getLastUploadedChunkIndex())
                .status(session.getStatus())
                .errorMessage(session.getErrorMessage())
                .expiredAt(session.getExpiredAt())
                .createdAt(session.getCreatedAt())
                .updatedAt(session.getUpdatedAt())
                .build();
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
