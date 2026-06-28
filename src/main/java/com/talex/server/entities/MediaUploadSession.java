package com.talex.server.entities;

import com.talex.server.entities.series.Episode;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaUploadSessionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
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
        name = "media_upload_sessions",
        indexes = {
                @Index(name = "idx_media_upload_sessions_media_deleted", columnList = "media_id,is_deleted"),
                @Index(name = "idx_media_upload_sessions_status_expired_deleted", columnList = "status,expired_at,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadSession extends BaseAudit {
    @Id
    @Column(name = "upload_session_id", length = 80)
    private String uploadSessionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "media_id")
    private Media media;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;

    @Column(name = "creator_id")
    private String creatorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private MediaProvider provider = MediaProvider.CLOUDINARY;

    @Column(name = "provider_public_id", nullable = false)
    private String providerPublicId;

    @Column(name = "provider_delivery_type", length = 40)
    private String providerDeliveryType;

    @Column(name = "upload_unique_id", nullable = false, length = 100)
    private String uploadUniqueId;

    @Column(name = "file_name", nullable = false)
    private String fileName;

    @Column(name = "file_size", nullable = false)
    private Long fileSize;

    @Column(name = "mime_type", nullable = false, length = 120)
    private String mimeType;

    @Column(name = "chunk_size", nullable = false)
    private Long chunkSize;

    @Column(name = "uploaded_bytes", nullable = false)
    private Long uploadedBytes = 0L;

    @Column(name = "total_chunks")
    private Integer totalChunks;

    @Column(name = "last_uploaded_chunk_index")
    private Integer lastUploadedChunkIndex;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private MediaUploadSessionStatus status = MediaUploadSessionStatus.INITIATED;

    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    @Column(name = "expired_at")
    private LocalDateTime expiredAt;
}
