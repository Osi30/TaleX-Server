package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.media.MediaPurgeLog;
import com.talex.server.repositories.media.ContentCensorshipRepository;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaPlaybackSessionRepository;
import com.talex.server.repositories.media.MediaPurgeLogRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.media.MediaUploadSessionRepository;
import com.talex.server.repositories.media.ViolationDetailRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * IRREVERSIBLE hard-delete of a single Media row and all its child rows in FK-safe order, for the
 * admin permanent-purge action (legal/DMCA/GDPR). Mirrors {@code ContentCascadeDeleteHelper}'s
 * repository-only dependency style (no service deps → no circular deps), but performs a true
 * DELETE instead of soft-delete.
 * <p>
 * This helper covers ONLY the Postgres side. The caller orchestrates the external side-effects
 * (S3 asset purge, Milvus fingerprint delete) AFTER this transaction commits — they are
 * best-effort and non-transactional, so a DB-consistent state is reached first.
 * <p>
 * The purge-source guard (block if the media is referenced as {@code source_media_id} of another
 * media's violation) is enforced by the caller BEFORE invoking this helper.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class MediaHardDeleteHelper {

    private final MediaRepository mediaRepository;
    private final MediaPurgeLogRepository mediaPurgeLogRepository;
    private final MediaCopyrightRepository mediaCopyrightRepository;
    private final ContentCensorshipRepository contentCensorshipRepository;
    private final ViolationDetailRepository violationDetailRepository;
    private final MediaPlaybackSessionRepository mediaPlaybackSessionRepository;
    private final MediaUploadSessionRepository mediaUploadSessionRepository;

    /**
     * Writes the audit snapshot, then hard-deletes every child row and finally the media row.
     * Runs in its own transaction so all Postgres deletes commit atomically.
     *
     * @param media   the media to purge (already loaded, may be soft-deleted)
     * @param adminId account id of the admin triggering the purge
     * @param reason  mandatory justification, persisted to the audit log
     */
    @Transactional
    public void hardDelete(Media media, String adminId, String reason) {
        String mediaId = media.getMediaId();
        String episodeId = media.getEpisode() != null ? media.getEpisode().getEpisodeId() : null;

        // 1) Audit snapshot FIRST — the append-only record must survive even if a later step fails.
        MediaPurgeLog purgeLog = MediaPurgeLog.builder()
                .mediaId(mediaId)
                .episodeId(episodeId)
                .creatorId(media.getCreatorId())
                .fileUrl(media.getFileUrl())
                .providerPublicId(media.getProviderPublicId())
                .fileSize(media.getFileSize())
                .adminAccountId(adminId)
                .reason(reason)
                .build();
        mediaPurgeLogRepository.save(purgeLog);

        // 2) violation_detail (children of content_censorship) — bulk delete bypasses the JPA
        //    orphanRemoval cascade, so remove these explicitly BEFORE their parent censorship rows.
        violationDetailRepository.deleteAllByCensorshipMediaId(mediaId);

        // 3) content_censorship (moderation results where this media is the target)
        contentCensorshipRepository.deleteAllByMedia_MediaId(mediaId);

        // 4) media_copyright where this media is the TARGET. (Rows where it is the SOURCE are the
        //    blocking condition checked by the caller — never reached here.)
        mediaCopyrightRepository.deleteAllByMedia_MediaId(mediaId);

        // 5) media_upload_sessions (transient upload bookkeeping)
        mediaUploadSessionRepository.deleteAllByMedia_MediaId(mediaId);

        // 6) media_playback_sessions (ephemeral tokens — active ones already revoked by caller)
        mediaPlaybackSessionRepository.deleteAllByMedia_MediaId(mediaId);

        // 7) the media row itself
        mediaRepository.delete(media);

        log.warn("Media HARD-PURGED. mediaId={} episodeId={} adminId={} reason='{}'",
                mediaId, episodeId, adminId, reason);
    }
}
