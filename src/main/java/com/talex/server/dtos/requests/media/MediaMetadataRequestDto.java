package com.talex.server.dtos.requests.media;

import com.talex.server.enums.media.MediaPlaybackPolicy;
import com.talex.server.enums.media.MediaProtectionType;
import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaMetadataRequestDto {
    @NotBlank
    private String fileUrl;

    @NotNull
    private MediaType mediaType;

    @NotBlank
    private String mimeType;

    @NotNull
    @PositiveOrZero
    private Long fileSize;

    private String checksum;
    private String externalPublicId;
    private String storageProvider;
    private MediaProvider provider;
    private String providerAssetId;
    private String providerPublicId;
    private String providerDeliveryType;
    private String originalUrl;
    private String playbackUrl;
    private String hlsUrl;
    private String thumbnailUrl;
    private String previewUrl;
    private String format;
    private MediaProtectionType protectionType;
    private MediaPlaybackPolicy playbackPolicy;
    private Integer width;
    private Integer height;
    private String resolution;
    private Long duration;
    private Integer displayOrder;
    private String actorId;
}
