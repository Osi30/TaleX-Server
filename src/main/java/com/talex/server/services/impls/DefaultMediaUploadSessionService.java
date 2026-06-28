package com.talex.server.services.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.requests.MediaUploadCompleteRequestDto;
import com.talex.server.dtos.requests.MediaUploadFailRequestDto;
import com.talex.server.dtos.requests.MediaUploadProgressRequestDto;
import com.talex.server.dtos.requests.VideoUploadSessionRequestDto;
import com.talex.server.dtos.responses.MediaResponseDto;
import com.talex.server.dtos.responses.MediaUploadSessionResponseDto;
import com.talex.server.dtos.responses.VideoUploadSessionResponseDto;
import com.talex.server.entities.series.Episode;
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
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.repositories.MediaUploadSessionRepository;
import com.talex.server.services.MediaService;
import com.talex.server.services.MediaUploadSessionService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.SignedUploadParams;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMediaUploadSessionService implements MediaUploadSessionService {
    private static final long MB = 1024L * 1024L;
    private static final List<MediaStatus> VIDEO_UPLOAD_BLOCKING_STATUSES = List.of(
            MediaStatus.ACTIVE,
            MediaStatus.HLS_READY,
            MediaStatus.HLS_PROCESSING);
    private static final List<MediaStatus> STALE_VIDEO_UPLOAD_STATUSES = List.of(MediaStatus.PROCESSING);
    private static final Set<MediaUploadSessionStatus> TERMINAL_UPLOAD_SESSION_STATUSES = EnumSet.of(
            MediaUploadSessionStatus.COMPLETED,
            MediaUploadSessionStatus.FAILED,
            MediaUploadSessionStatus.CANCELLED,
            MediaUploadSessionStatus.EXPIRED);
    private static final Set<MediaUploadSessionStatus> ALLOWED_PROGRESS_STATUSES = EnumSet.of(
            MediaUploadSessionStatus.UPLOADING,
            MediaUploadSessionStatus.PAUSED);

    private final MediaUploadSessionRepository uploadSessionRepository;
    private final MediaRepository mediaRepository;
    private final EpisodeRepository episodeRepository;
    private final MediaService mediaService;
    private final MediaProviderService mediaProviderService;
    private final MediaProperties mediaProperties;
    private final CloudinaryHlsReconcileService cloudinaryHlsReconcileService;
    private final MediaUploadProgressCache uploadProgressCache;

    @Transactional
    @Override
    public VideoUploadSessionResponseDto createVideoUploadSession(String episodeId, VideoUploadSessionRequestDto request) {
        Episode episode = lockActiveEpisode(episodeId);
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
        media.setProvider(mediaProperties.getProvider());
        media.setProviderDeliveryType(
                mediaProperties.getProvider() == MediaProvider.CLOUDINARY
                        ? mediaProperties.getCloudinary().getProviderDeliveryType()
                        : null);
        media.setProtectionType(protectionType);
        media.setPlaybackPolicy(playbackPolicy);
        media.setTokenTtlSeconds(mediaProperties.getSignedPlaybackTtlSeconds().intValue());
        media.setStatus(MediaStatus.PROCESSING);
        media.markCreatedBy(request.getActorId());
        media = mediaRepository.save(media);

        String providerPublicId = mediaProviderService.buildVideoPublicId(episodeId, media.getMediaId());
        media.setProviderPublicId(providerPublicId);
        media.setStorageProvider(mediaProperties.getProvider().name());
        media.setExternalPublicId(providerPublicId);

        MediaUploadSession session = new MediaUploadSession();
        session.setUploadSessionId(UUID.randomUUID().toString());
        session.setMedia(media);
        session.setEpisode(episode);
        session.setCreatorId(blankToNull(request.getCreatorId()));
        session.setProvider(mediaProperties.getProvider());
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
        MediaUploadSession session = findSession(uploadSessionId);
        return toResponse(session, latestProgressFor(session));
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto updateProgress(String uploadSessionId, MediaUploadProgressRequestDto request) {
        MediaUploadSession session = findSession(uploadSessionId);
        CachedMediaUploadProgress currentProgress = latestProgressFor(session);
        MediaUploadSessionStatus nextStatus = request.getStatus() == null
                ? MediaUploadSessionStatus.UPLOADING
                : request.getStatus();

        if (!ALLOWED_PROGRESS_STATUSES.contains(nextStatus)) {
            throw ContentModuleException.badRequest("Upload progress status must be UPLOADING or PAUSED");
        }
        if (request.getUploadedBytes() > session.getFileSize()) {
            throw ContentModuleException.badRequest("uploadedBytes cannot exceed fileSize");
        }
        if (TERMINAL_UPLOAD_SESSION_STATUSES.contains(session.getStatus())) {
            throw ContentModuleException.badRequest("Upload session cannot accept progress in status " + session.getStatus());
        }
        validateChunkIndex(session, request.getLastUploadedChunkIndex());

        long currentUploadedBytes = currentProgress.uploadedBytes() == null ? 0L : currentProgress.uploadedBytes();
        if (request.getUploadedBytes() < currentUploadedBytes) {
            log.debug("Ignored stale video upload progress. uploadSessionId={} currentUploadedBytes={} requestedUploadedBytes={}",
                    uploadSessionId, currentUploadedBytes, request.getUploadedBytes());
            return toResponse(session, currentProgress);
        }

        if (isSameProgress(currentProgress, request, nextStatus)) {
            return toResponse(session, currentProgress);
        }

        CachedMediaUploadProgress nextProgress = new CachedMediaUploadProgress(
                request.getUploadedBytes(),
                request.getLastUploadedChunkIndex(),
                nextStatus,
                request.getActorId());
        if (!uploadProgressCache.put(uploadSessionId, nextProgress, session)) {
            session.setUploadedBytes(nextProgress.uploadedBytes());
            session.setLastUploadedChunkIndex(nextProgress.lastUploadedChunkIndex());
            session.setStatus(nextProgress.status());
            session.markUpdatedBy(nextProgress.actorId());
            session = uploadSessionRepository.save(session);
        }
        log.debug("Video upload progress updated. uploadSessionId={} uploadedBytes={}",
                uploadSessionId, request.getUploadedBytes());
        return toResponse(session, nextProgress);
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto pause(String uploadSessionId, String actorId) {
        MediaUploadSession session = findSession(uploadSessionId);
        applyCachedProgress(session);
        session.setStatus(MediaUploadSessionStatus.PAUSED);
        session.markUpdatedBy(actorId);
        log.info("Video upload paused. uploadSessionId={}", uploadSessionId);
        session = uploadSessionRepository.save(session);
        uploadProgressCache.delete(uploadSessionId);
        return toResponse(session);
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto fail(String uploadSessionId, MediaUploadFailRequestDto request) {
        MediaUploadSession session = findSession(uploadSessionId);
        applyCachedProgress(session);
        session.setStatus(MediaUploadSessionStatus.FAILED);
        session.setErrorMessage(request == null ? null : blankToNull(request.getErrorMessage()));
        session.markUpdatedBy(request == null ? null : request.getActorId());
        if (session.getMedia() != null) {
            session.getMedia().setStatus(MediaStatus.FAILED);
            session.getMedia().setErrorMessage(session.getErrorMessage());
            mediaRepository.save(session.getMedia());
        }
        log.warn("Video upload failed. uploadSessionId={} error={}", uploadSessionId, session.getErrorMessage());
        session = uploadSessionRepository.save(session);
        uploadProgressCache.delete(uploadSessionId);
        return toResponse(session);
    }

    @Transactional
    @Override
    public MediaUploadSessionResponseDto cancel(String uploadSessionId, String actorId) {
        MediaUploadSession session = findSession(uploadSessionId);
        applyCachedProgress(session);
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
        session = uploadSessionRepository.save(session);
        uploadProgressCache.delete(uploadSessionId);
        return toResponse(session);
    }

    @Transactional
    @Override
    public MediaResponseDto complete(String uploadSessionId, MediaUploadCompleteRequestDto request) {
        MediaUploadSession session = findSession(uploadSessionId);
        applyCachedProgress(session);
        lockActiveEpisode(session.getEpisode().getEpisodeId());
        if (session.getStatus() == MediaUploadSessionStatus.COMPLETED) {
            validateCompletedRequestMatchesSession(session, request);
            if (session.getMedia() == null) {
                throw ContentModuleException.notFound("Reserved media not found for upload session");
            }
            return mediaService.toResponse(session.getMedia());
        }
        if (session.getStatus() == MediaUploadSessionStatus.FAILED
                || session.getStatus() == MediaUploadSessionStatus.CANCELLED
                || session.getStatus() == MediaUploadSessionStatus.EXPIRED) {
            throw ContentModuleException.badRequest("Upload session cannot complete in status " + session.getStatus());
        }
        validateCompletedRequestMatchesSession(session, request);

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
        log.info("Upload completed, HLS processing started. provider={} mediaId={} providerPublicId={}",
                media.getProvider(), media.getMediaId(), media.getProviderPublicId());
        if (media.getProvider() == MediaProvider.CLOUDINARY) {
            cloudinaryHlsReconcileService.notifyProcessingMedia();
        }

        session.setUploadedBytes(request.getBytes());
        session.setLastUploadedChunkIndex(session.getTotalChunks() == null ? null : session.getTotalChunks() - 1);
        session.setStatus(MediaUploadSessionStatus.COMPLETED);
        session.setErrorMessage(null);
        session.markUpdatedBy(request.getActorId());
        uploadSessionRepository.save(session);
        uploadProgressCache.delete(uploadSessionId);

        log.info("Video upload completed. uploadSessionId={} mediaId={} providerPublicId={}",
                uploadSessionId, media.getMediaId(), media.getProviderPublicId());
        return mediaService.toResponse(media);
    }

    private void validateCompletedRequestMatchesSession(MediaUploadSession session, MediaUploadCompleteRequestDto request) {
        if (!session.getProviderPublicId().equals(request.getPublicId())) {
            throw ContentModuleException.badRequest("Provider publicId does not match upload session");
        }
        if (request.getBytes() > session.getFileSize()) {
            throw ContentModuleException.badRequest("Completed byte size cannot exceed declared fileSize");
        }
    }

    private void validateChunkIndex(MediaUploadSession session, Integer lastUploadedChunkIndex) {
        if (lastUploadedChunkIndex == null) {
            return;
        }
        if (lastUploadedChunkIndex < -1) {
            throw ContentModuleException.badRequest("lastUploadedChunkIndex cannot be less than -1");
        }
        if (session.getTotalChunks() != null && lastUploadedChunkIndex >= session.getTotalChunks()) {
            throw ContentModuleException.badRequest("lastUploadedChunkIndex cannot exceed totalChunks");
        }
    }

    private boolean isSameProgress(
            CachedMediaUploadProgress currentProgress,
            MediaUploadProgressRequestDto request,
            MediaUploadSessionStatus nextStatus) {
        return java.util.Objects.equals(currentProgress.uploadedBytes(), request.getUploadedBytes())
                && java.util.Objects.equals(currentProgress.lastUploadedChunkIndex(), request.getLastUploadedChunkIndex())
                && currentProgress.status() == nextStatus
                && java.util.Objects.equals(currentProgress.actorId(), request.getActorId());
    }

    private CachedMediaUploadProgress latestProgressFor(MediaUploadSession session) {
        CachedMediaUploadProgress persistedProgress = new CachedMediaUploadProgress(
                session.getUploadedBytes(),
                session.getLastUploadedChunkIndex(),
                session.getStatus(),
                session.getUpdatedBy());
        if (TERMINAL_UPLOAD_SESSION_STATUSES.contains(session.getStatus())) {
            return persistedProgress;
        }
        return uploadProgressCache.get(session.getUploadSessionId())
                .filter(cachedProgress -> isUsableCachedProgress(session, cachedProgress))
                .orElse(persistedProgress);
    }

    private boolean isUsableCachedProgress(MediaUploadSession session, CachedMediaUploadProgress cachedProgress) {
        if (cachedProgress.uploadedBytes() == null || cachedProgress.uploadedBytes() > session.getFileSize()) {
            return false;
        }
        if (cachedProgress.status() == null || !ALLOWED_PROGRESS_STATUSES.contains(cachedProgress.status())) {
            return false;
        }
        long persistedUploadedBytes = session.getUploadedBytes() == null ? 0L : session.getUploadedBytes();
        if (cachedProgress.uploadedBytes() < persistedUploadedBytes) {
            return false;
        }
        try {
            validateChunkIndex(session, cachedProgress.lastUploadedChunkIndex());
            return true;
        } catch (ContentModuleException ex) {
            return false;
        }
    }

    private void applyCachedProgress(MediaUploadSession session) {
        CachedMediaUploadProgress cachedProgress = latestProgressFor(session);
        session.setUploadedBytes(cachedProgress.uploadedBytes());
        session.setLastUploadedChunkIndex(cachedProgress.lastUploadedChunkIndex());
        session.setStatus(cachedProgress.status());
    }

    private void reconcileExistingVideoMedia(String episodeId, String actorId) {
        boolean hasBlockingVideo = mediaRepository.existsByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                episodeId,
                MediaType.VIDEO,
                VIDEO_UPLOAD_BLOCKING_STATUSES);
        if (hasBlockingVideo) {
            throw ContentModuleException.conflict("Video episode already has a video media or HLS processing is still running. Delete it before uploading a replacement.");
        }

        var staleProcessingVideos = mediaRepository
                .findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                        episodeId,
                        MediaType.VIDEO,
                        STALE_VIDEO_UPLOAD_STATUSES);
        if (staleProcessingVideos.isEmpty()) {
            return;
        }

        List<String> staleMediaIds = staleProcessingVideos.stream()
                .map(Media::getMediaId)
                .toList();
        List<MediaUploadSession> sessionsToCancel = new ArrayList<>();
        uploadSessionRepository.findAllByMedia_MediaIdInAndIsDeletedFalse(staleMediaIds)
                .stream()
                .filter(session -> session.getStatus() != MediaUploadSessionStatus.COMPLETED)
                .forEach(session -> {
                    session.setStatus(MediaUploadSessionStatus.CANCELLED);
                    session.setErrorMessage("Superseded by a new upload session");
                    session.markUpdatedBy(actorId);
                    sessionsToCancel.add(session);
                });
        if (!sessionsToCancel.isEmpty()) {
            uploadSessionRepository.saveAll(sessionsToCancel);
        }

        for (Media media : staleProcessingVideos) {
            media.setStatus(MediaStatus.DELETED);
            media.setErrorMessage("Superseded by a new upload session");
            media.softDelete(actorId);
            log.info("Cancelled stale processing video before creating a new upload session. mediaId={} episodeId={}",
                    media.getMediaId(), episodeId);
        }
        mediaRepository.saveAll(staleProcessingVideos);
    }

    private MediaUploadSession findSession(String uploadSessionId) {
        return uploadSessionRepository.findByUploadSessionIdAndIsDeletedFalse(uploadSessionId)
                .orElseThrow(() -> ContentModuleException.notFound("Upload session not found: " + uploadSessionId));
    }

    private Episode lockActiveEpisode(String episodeId) {
        return episodeRepository.lockByEpisodeIdAndIsDeletedFalse(episodeId)
                .orElseThrow(() -> ContentModuleException.notFound("Episode not found: " + episodeId));
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
        return toResponse(session, new CachedMediaUploadProgress(
                session.getUploadedBytes(),
                session.getLastUploadedChunkIndex(),
                session.getStatus(),
                session.getUpdatedBy()));
    }

    private MediaUploadSessionResponseDto toResponse(MediaUploadSession session, CachedMediaUploadProgress progress) {
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
                .uploadedBytes(progress.uploadedBytes())
                .totalChunks(session.getTotalChunks())
                .lastUploadedChunkIndex(progress.lastUploadedChunkIndex())
                .status(progress.status())
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
