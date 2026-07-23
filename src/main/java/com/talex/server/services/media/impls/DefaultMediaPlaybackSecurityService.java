package com.talex.server.services.media.impls;

import com.talex.server.configs.properties.MediaProperties;
import com.talex.server.dtos.responses.media.DrmPlaybackConfigDto;
import com.talex.server.dtos.responses.series.EpisodePlaybackResponseDto;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaPlaybackSession;
import com.talex.server.enums.media.MediaPlaybackPolicy;
import com.talex.server.enums.media.MediaPlaybackSessionStatus;
import com.talex.server.enums.media.MediaProtectionType;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.media.MediaPlaybackSessionRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.DrmLicenseService;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaProtectionService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.series.EpisodeService;
import com.talex.server.services.auth.PlaybackAuthorizationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.HexFormat;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMediaPlaybackSecurityService implements MediaPlaybackSecurityService, MediaProtectionService {
    private static final List<MediaStatus> PLAYBACK_VIDEO_STATUSES = List.of(
            MediaStatus.PROCESSING,
            MediaStatus.HLS_PROCESSING,
            MediaStatus.HLS_READY,
            MediaStatus.ACTIVE,
            MediaStatus.FAILED);

    private final EpisodeService episodeService;
    private final MediaRepository mediaRepository;
    private final MediaPlaybackSessionRepository playbackSessionRepository;
    private final PlaybackAuthorizationService playbackAuthorizationService;
    private final MediaProviderService mediaProviderService;
    private final DrmLicenseService drmLicenseService;
    private final MediaProperties mediaProperties;

    @Transactional
    @Override
    public EpisodePlaybackResponseDto getEpisodePlayback(String episodeId, String viewerId, String ipAddress, String userAgent) {
        return buildEpisodePlayback(
                episodeService.findPublicEntity(episodeId),
                episodeId,
                viewerId,
                ipAddress,
                userAgent);
    }

    @Transactional
    @Override
    public EpisodePlaybackResponseDto getCreatorEpisodePlayback(String episodeId, String viewerId, String ipAddress, String userAgent) {
        return buildEpisodePlayback(
                episodeService.findActiveEntity(episodeId),
                episodeId,
                viewerId,
                ipAddress,
                userAgent);
    }

    private EpisodePlaybackResponseDto buildEpisodePlayback(
            Episode episode,
            String episodeId,
            String viewerId,
            String ipAddress,
            String userAgent) {
        if (episode.getStatus() == com.talex.server.enums.series.EpisodeStatus.SCHEDULED) {
            throw ContentModuleException.forbidden("Cannot playback media for scheduled episode");
        }
        boolean isEntitled = playbackAuthorizationService.canViewEpisode(viewerId, episodeId);

        Media media = mediaRepository
                .findFirstByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalseOrderByCreatedAtDesc(
                        episodeId,
                        MediaType.VIDEO,
                        PLAYBACK_VIDEO_STATUSES)
                .orElseThrow(() -> ContentModuleException.notFound("Playable video media not found for episode: " + episodeId));

        if (media.getStatus() == MediaStatus.FAILED) {
            log.warn("PLAYBACK_REQUESTED_BEFORE_READY episodeId={} mediaId={} status={} error={}",
                    episodeId, media.getMediaId(), media.getStatus(), media.getErrorMessage());
            throw ContentModuleException.badRequest("VIDEO_FAILED");
        }
        if (media.getStatus() == MediaStatus.PROCESSING || media.getStatus() == MediaStatus.HLS_PROCESSING) {
            log.info("PLAYBACK_REQUESTED_BEFORE_READY episodeId={} mediaId={} status={}",
                    episodeId, media.getMediaId(), media.getStatus());
            throw ContentModuleException.badRequest("VIDEO_PROCESSING");
        }
        if (media.getStatus() != MediaStatus.HLS_READY && media.getStatus() != MediaStatus.ACTIVE) {
            log.info("PLAYBACK_REQUESTED_BEFORE_READY episodeId={} mediaId={} status={}",
                    episodeId, media.getMediaId(), media.getStatus());
            throw ContentModuleException.badRequest("VIDEO_NOT_READY");
        }

        if (!isEntitled) {
            String previewUrl = media.getPreviewUrl();
            if (previewUrl == null || previewUrl.isBlank()) {
                throw ContentModuleException.forbidden("PLAYBACK_NOT_ENTITLED");
            }
            
            LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(resolveTtl(media));
            return EpisodePlaybackResponseDto.builder()
                    .episodeId(episodeId)
                    .mediaId(media.getMediaId())
                    .mediaType(media.getMediaType())
                    .playbackType("HLS")
                    .provider(media.getProvider())
                    .protectionType(MediaProtectionType.NONE)
                    .hlsUrl(previewUrl)
                    .playbackUrl(previewUrl)
                    .thumbnailUrl(media.getThumbnailUrl())
                    .duration(10L)
                    .expiresAt(expiresAt)
                    .isLocked(true)
                    .build();
        }

        MediaProtectionType protectionType = getProtectionType(media);
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(resolveTtl(media));

        if (protectionType == MediaProtectionType.DRM_MULTI || media.getPlaybackPolicy() == MediaPlaybackPolicy.DRM_REQUIRED) {
            if (!Boolean.TRUE.equals(mediaProperties.getEnableDrm())) {
                log.warn("DRM playback requested but DRM is disabled. mediaId={}", media.getMediaId());
                throw ContentModuleException.badRequest("DRM_NOT_CONFIGURED");
            }
            DrmPlaybackConfigDto drm = generateDrmPlaybackConfig(media, viewerId);
            return EpisodePlaybackResponseDto.builder()
                    .episodeId(episodeId)
                    .mediaId(media.getMediaId())
                    .mediaType(media.getMediaType())
                    .playbackType("HLS_OR_DASH")
                    .provider(media.getProvider())
                    .protectionType(protectionType)
                    .manifestUrl(media.getPlaybackUrl())
                    .thumbnailUrl(media.getThumbnailUrl())
                    .duration(media.getDuration())
                    .expiresAt(expiresAt)
                    .drm(drm)
                    .token(drmLicenseService.generateLicenseToken(media, viewerId))
                    .build();
        }

        String playbackUrl;
        try {
            playbackUrl = protectionType == MediaProtectionType.NONE || media.getPlaybackPolicy() == MediaPlaybackPolicy.PUBLIC
                    ? firstNonBlank(media.getHlsUrl(), media.getPlaybackUrl(), media.getFileUrl())
                    : generateSignedPlayback(media, expiresAt);
        } catch (ContentModuleException ex) {
            log.info("PLAYBACK_REQUESTED_BEFORE_READY episodeId={} mediaId={} status={} reason={}",
                    episodeId, media.getMediaId(), media.getStatus(), ex.getMessage());
            throw ContentModuleException.badRequest("VIDEO_NOT_READY");
        }

        MediaPlaybackSession session = new MediaPlaybackSession();
        session.setPlaybackSessionId(UUID.randomUUID().toString());
        session.setMedia(media);
        session.setEpisode(episode);
        session.setViewerId(blankToNull(viewerId));
        session.setProvider(media.getProvider());
        session.setProtectionType(protectionType);
        session.setPlaybackUrl(playbackUrl);
        session.setTokenId(UUID.randomUUID().toString());
        session.setExpiresAt(expiresAt);
        session.setIpAddressHash(hashNullable(ipAddress));
        session.setUserAgentHash(hashNullable(userAgent));
        session.setStatus(MediaPlaybackSessionStatus.ACTIVE);
        playbackSessionRepository.save(session);

        log.info("Playback URL issued. episodeId={} mediaId={} protectionType={} expiresAt={}",
                episodeId, media.getMediaId(), protectionType, expiresAt);

        // Sign thumbnail URL for protected content
        String thumbnailUrl = media.getThumbnailUrl();
        if (protectionType != MediaProtectionType.NONE
                && media.getPlaybackPolicy() != MediaPlaybackPolicy.PUBLIC
                && thumbnailUrl != null && !thumbnailUrl.isBlank()) {
            thumbnailUrl = mediaProviderService.signSingleUrl(thumbnailUrl, expiresAt);
        }

        return EpisodePlaybackResponseDto.builder()
                .episodeId(episodeId)
                .mediaId(media.getMediaId())
                .mediaType(media.getMediaType())
                .playbackType("HLS")
                .provider(media.getProvider())
                .protectionType(protectionType)
                .hlsUrl(playbackUrl)
                .playbackUrl(playbackUrl)
                .thumbnailUrl(thumbnailUrl)
                .duration(media.getDuration())
                .expiresAt(expiresAt)
                .build();
    }

    @Override
    public MediaProtectionType getProtectionType(Media media) {
        return media.getProtectionType() == null ? MediaProtectionType.NONE : media.getProtectionType();
    }

    @Override
    public String generateSignedPlayback(Media media, LocalDateTime expiresAt) {
        return mediaProviderService.buildSignedHlsUrl(media, expiresAt);
    }

    @Override
    public DrmPlaybackConfigDto generateDrmPlaybackConfig(Media media, String viewerId) {
        return drmLicenseService.getLicenseUrls(media, viewerId);
    }

    @Transactional
    @Override
    public void revokeActiveSessions(Media media) {
        var sessions = playbackSessionRepository
                .findAllByMedia_MediaIdAndStatusAndIsDeletedFalse(media.getMediaId(), MediaPlaybackSessionStatus.ACTIVE);
        sessions.forEach(session -> session.setStatus(MediaPlaybackSessionStatus.REVOKED));
        if (!sessions.isEmpty()) {
            playbackSessionRepository.saveAll(sessions);
        }
        log.info("Playback sessions revoked. mediaId={}", media.getMediaId());
    }

    @Transactional
    @Override
    public void revokePlayback(Media media) {
        revokeActiveSessions(media);
    }

    @Transactional
    @Override
    public int expireOldSessions() {
        var sessions = playbackSessionRepository.findAllByStatusAndExpiresAtBeforeAndIsDeletedFalse(
                MediaPlaybackSessionStatus.ACTIVE,
                LocalDateTime.now());
        sessions.forEach(session -> session.setStatus(MediaPlaybackSessionStatus.EXPIRED));
        if (!sessions.isEmpty()) {
            playbackSessionRepository.saveAll(sessions);
        }
        if (!sessions.isEmpty()) {
            log.info("Expired playback sessions. count={}", sessions.size());
        }
        return sessions.size();
    }

    private long resolveTtl(Media media) {
        if (media.getTokenTtlSeconds() != null && media.getTokenTtlSeconds() > 0) {
            return media.getTokenTtlSeconds();
        }
        return mediaProperties.getSignedPlaybackTtlSeconds();
    }

    private String firstNonBlank(String... values) {
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        throw ContentModuleException.notFound("Playback URL is not available yet");
    }

    private String hashNullable(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException ex) {
            return null;
        }
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
