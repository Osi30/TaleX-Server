package com.talex.server.entities;

import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.MediaPlaybackPolicy;
import com.talex.server.enums.MediaProtectionType;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "media",
        indexes = {
                @Index(name = "idx_media_episode_deleted_order", columnList = "episode_id,is_deleted,display_order"),
                @Index(name = "idx_media_episode_status_deleted_order", columnList = "episode_id,status,is_deleted,display_order"),
                @Index(name = "idx_media_episode_type_status_deleted", columnList = "episode_id,media_type,status,is_deleted"),
                @Index(name = "idx_media_episode_type_status_approval_deleted", columnList = "episode_id,media_type,status,approval_status,is_deleted"),
                @Index(name = "idx_media_checksum_deleted", columnList = "checksum,is_deleted"),
                @Index(name = "idx_media_provider_public_deleted", columnList = "provider_public_id,is_deleted"),
                @Index(name = "idx_media_provider_status_updated_deleted", columnList = "provider,status,updated_at,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Media extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "media_id")
    private String mediaId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Enumerated(EnumType.STRING)
    @Column(name = "media_type", nullable = false, length = 30)
    private MediaType mediaType;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "file_url", columnDefinition = "TEXT")
    private String fileUrl;

    @Column(name = "external_public_id")
    private String externalPublicId;

    @Column(name = "storage_provider", length = 50)
    private String storageProvider;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(length = 128)
    private String checksum;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MediaProvider provider = MediaProvider.URL;

    @Column(name = "provider_asset_id")
    private String providerAssetId;

    @Column(name = "provider_public_id")
    private String providerPublicId;

    @Column(name = "provider_delivery_type", length = 40)
    private String providerDeliveryType;

    @Column(name = "original_url", columnDefinition = "TEXT")
    private String originalUrl;

    @Column(name = "playback_url", columnDefinition = "TEXT")
    private String playbackUrl;

    @Column(name = "hls_url", columnDefinition = "TEXT")
    private String hlsUrl;

    @Column(name = "signed_playback_url", columnDefinition = "TEXT")
    private String signedPlaybackUrl;

    @Column(name = "thumbnail_url", columnDefinition = "TEXT")
    private String thumbnailUrl;

    @Column(name = "preview_url", columnDefinition = "TEXT")
    private String previewUrl;

    @Column(length = 30)
    private String format;

    @Enumerated(EnumType.STRING)
    @Column(name = "protection_type", nullable = false, length = 40)
    private MediaProtectionType protectionType = MediaProtectionType.NONE;

    @Enumerated(EnumType.STRING)
    @Column(name = "playback_policy", nullable = false, length = 40)
    private MediaPlaybackPolicy playbackPolicy = MediaPlaybackPolicy.PUBLIC;

    @Column(name = "drm_provider")
    private String drmProvider;

    @Column(name = "drm_key_id")
    private String drmKeyId;

    @Column(name = "drm_license_url", columnDefinition = "TEXT")
    private String drmLicenseUrl;

    @Column(name = "drm_certificate_url", columnDefinition = "TEXT")
    private String drmCertificateUrl;

    @Column(name = "token_ttl_seconds")
    private Integer tokenTtlSeconds;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "pending_delete", nullable = false)
    private Boolean pendingDelete = false;

    private Integer width;

    private Integer height;

    @Column(length = 50)
    private String resolution;

    private Long duration;

    @Column(name = "display_order")
    private Integer displayOrder;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaStatus status = MediaStatus.PROCESSING;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30, columnDefinition = "varchar(30) default 'PENDING_REVIEW'")
    private ContentApprovalStatus approvalStatus = ContentApprovalStatus.PENDING_REVIEW;

    @Column(name = "approval_reviewed_at")
    private LocalDateTime approvalReviewedAt;

    @Column(name = "approval_reviewed_by")
    private String approvalReviewedBy;
}
