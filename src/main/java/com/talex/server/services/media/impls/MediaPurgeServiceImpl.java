package com.talex.server.services.media.impls;

import com.talex.server.entities.media.Media;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.media.MediaCopyrightRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.services.media.ContentPipelineService;
import com.talex.server.services.media.MediaPlaybackSecurityService;
import com.talex.server.services.media.MediaProviderService;
import com.talex.server.services.media.MediaPurgeService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * Orchestrates the irreversible admin media purge. Ordering is deliberate:
 * <ol>
 *   <li>Block if the media is the SOURCE of another media's violation (evidence protection).</li>
 *   <li>Revoke active playback sessions.</li>
 *   <li>Postgres hard-delete + audit log, in a single committed transaction (via
 *       {@link MediaHardDeleteHelper}).</li>
 *   <li>ONLY AFTER commit: purge S3 assets and Milvus fingerprint, both best-effort.</li>
 * </ol>
 * External side-effects run after the DB commit so that if S3/Kafka fail, the database is already in
 * a consistent state and the audit log records the intent — residual storage artifacts can be
 * cleaned up by re-running or manual ops. Doing externals first would risk deleting real files for a
 * media row that then fails to delete.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class MediaPurgeServiceImpl implements MediaPurgeService {

    private final MediaRepository mediaRepository;
    private final MediaCopyrightRepository mediaCopyrightRepository;
    private final MediaHardDeleteHelper mediaHardDeleteHelper;
    private final MediaPlaybackSecurityService playbackSecurityService;
    private final MediaProviderService mediaProviderService;
    private final ContentPipelineService contentPipelineService;

    @Override
    public void purge(String mediaId, String adminId, String reason) {
        // Load including soft-deleted rows — an already-soft-deleted media can still be purged.
        Media media = mediaRepository.findByMediaId(mediaId)
                .orElseThrow(() -> ContentModuleException.notFound("Media not found: " + mediaId));

        // Guard: block purge if this media is referenced as the ORIGINAL SOURCE of another media's
        // copyright violation. Hard-deleting it would break the source_media_id link and erase the
        // evidence that the other media plagiarized this one. Admin must resolve those violation(s)
        // first.
        if (mediaCopyrightRepository.existsBySourceMedia_MediaId(mediaId)) {
            throw ContentModuleException.conflict(
                    "Cannot purge: this media is the source of another media's copyright-violation "
                            + "record. Resolve those violation(s) first.");
        }

        log.warn("Admin media PURGE requested: mediaId={} adminId={} reason='{}'", mediaId, adminId, reason);

        // Revoke active signed-playback sessions before deleting the rows.
        playbackSecurityService.revokeActiveSessions(media);

        // Postgres hard-delete + audit snapshot (own transaction, commits before externals run).
        mediaHardDeleteHelper.hardDelete(media, adminId, reason);

        // ── AFTER commit: best-effort external side-effects ─────────────────────────────────────

        // S3/CloudFront: delete the source object AND the entire MediaConvert output prefix.
        try {
            mediaProviderService.purgeAllAssets(media);
        } catch (Exception e) {
            log.warn("Media purge: S3 asset purge failed (DB already purged). mediaId={}", mediaId, e);
        }

        // Milvus: delete the fingerprint via the existing content-media-delete Kafka path. Cluster
        // ownership auto re-resolves to the next-earliest non-violation row (first-seen-wins).
        // notifyMediaDeleted is itself best-effort, but wrap defensively too.
        try {
            contentPipelineService.notifyMediaDeleted(mediaId);
        } catch (Exception e) {
            log.warn("Media purge: fingerprint delete notify failed (DB already purged). mediaId={}", mediaId, e);
        }

        log.warn("Admin media PURGE completed: mediaId={} adminId={}", mediaId, adminId);
    }
}
