package com.talex.server.services.media;

/**
 * Admin-only irreversible hard-purge of a single media (Postgres + Milvus fingerprint + S3/CloudFront
 * assets). Separate from the daily Creator soft-delete flow, which stays soft and keeps the
 * fingerprint on purpose. Intended for confirmed legal cases: DMCA takedown, court order, illegal
 * content, GDPR erasure.
 */
public interface MediaPurgeService {

    /**
     * Permanently erases the media and all its traces.
     *
     * @param mediaId the media to purge
     * @param adminId account id of the admin triggering the purge (for the audit log)
     * @param reason  mandatory justification, persisted to media_purge_log
     */
    void purge(String mediaId, String adminId, String reason);
}
