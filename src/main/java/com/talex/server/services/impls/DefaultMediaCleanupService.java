package com.talex.server.services.impls;

import com.talex.server.entities.MediaUploadSession;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaUploadSessionStatus;
import com.talex.server.repositories.MediaPlaybackSessionRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.repositories.MediaUploadSessionRepository;
import com.talex.server.services.MediaCleanupService;
import com.talex.server.services.MediaPlaybackSecurityService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class DefaultMediaCleanupService implements MediaCleanupService {
    private final MediaUploadSessionRepository uploadSessionRepository;
    private final MediaRepository mediaRepository;
    private final MediaPlaybackSecurityService playbackSecurityService;

    @Transactional
    @Override
    public int expireStaleUploadSessions() {
        List<MediaUploadSessionStatus> openStatuses = List.of(
                MediaUploadSessionStatus.INITIATED,
                MediaUploadSessionStatus.UPLOADING,
                MediaUploadSessionStatus.PAUSED,
                MediaUploadSessionStatus.FAILED);
        var sessions = uploadSessionRepository.findAllByStatusInAndExpiredAtBeforeAndIsDeletedFalse(
                openStatuses,
                LocalDateTime.now());
        List<com.talex.server.entities.Media> failedMedia = new ArrayList<>();
        sessions.forEach(session -> {
            session.setStatus(MediaUploadSessionStatus.EXPIRED);
            if (session.getMedia() != null && session.getMedia().getStatus() == MediaStatus.PROCESSING) {
                session.getMedia().setStatus(MediaStatus.FAILED);
                session.getMedia().setErrorMessage("Upload session expired before completion");
                failedMedia.add(session.getMedia());
            }
        });
        if (!failedMedia.isEmpty()) {
            mediaRepository.saveAll(failedMedia);
        }
        if (!sessions.isEmpty()) {
            uploadSessionRepository.saveAll(sessions);
        }
        if (!sessions.isEmpty()) {
            log.info("Expired stale upload sessions. count={}", sessions.size());
        }
        return sessions.size();
    }

    @Transactional
    @Override
    public int expirePlaybackSessions() {
        return playbackSecurityService.expireOldSessions();
    }
}
