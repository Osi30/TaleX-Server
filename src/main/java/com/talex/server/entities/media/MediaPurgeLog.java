package com.talex.server.entities.media;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "media_purge_log",
        indexes = {
                @Index(name = "idx_media_purge_log_media", columnList = "media_id"),
                @Index(name = "idx_media_purge_log_admin", columnList = "admin_account_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaPurgeLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "media_purge_log_id")
    private String mediaPurgeLogId;

    // The purged media's id — kept as a plain value (NOT an FK) because the media row is deleted.
    @Column(name = "media_id", nullable = false)
    private String mediaId;

    @Column(name = "episode_id")
    private String episodeId;

    @Column(name = "creator_id")
    private String creatorId;

    // Storage path/url of the erased asset (for the audit trail — the object itself is deleted).
    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "provider_public_id", columnDefinition = "TEXT")
    private String providerPublicId;

    @Column(name = "file_size")
    private Long fileSize;

    // Admin account that triggered the irreversible purge.
    @Column(name = "admin_account_id", nullable = false)
    private String adminAccountId;

    // Mandatory human-provided justification (legal case reference, court order id, etc.).
    @Column(name = "reason", nullable = false, columnDefinition = "TEXT")
    private String reason;

    @CreationTimestamp
    @Column(name = "purged_at", updatable = false)
    private LocalDateTime purgedAt;
}
