package com.talex.server.dtos.responses;

import com.talex.server.enums.MediaPlaybackPolicy;
import com.talex.server.enums.MediaProtectionType;
import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaResponseDto {
    private String mediaId;
    private String episodeId;
    private MediaType mediaType;
    private String mimeType;
    private String fileUrl;
    private String externalPublicId;
    private String storageProvider;
    private MediaProvider provider;
    private String providerAssetId;
    private String providerPublicId;
    private String providerDeliveryType;
    private String originalUrl;
    private String playbackUrl;
    private String hlsUrl;
    private String signedPlaybackUrl;
    private String thumbnailUrl;
    private String previewUrl;
    private String format;
    private MediaProtectionType protectionType;
    private MediaPlaybackPolicy playbackPolicy;
    private String drmProvider;
    private String drmKeyId;
    private String drmLicenseUrl;
    private String drmCertificateUrl;
    private Integer tokenTtlSeconds;
    private String errorMessage;
    private Boolean pendingDelete;
    private Long fileSize;
    private String checksum;
    private Integer width;
    private Integer height;
    private String resolution;
    private Long duration;
    private Integer displayOrder;
    private MediaStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String createdBy;
    private String updatedBy;
    private String deletedBy;
    private Boolean isDeleted;
}
