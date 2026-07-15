package com.talex.server.dtos.responses.media;

import com.talex.server.enums.media.MediaProvider;
import com.talex.server.enums.media.MediaUploadSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadSessionResponseDto {
    private String uploadSessionId;
    private String mediaId;
    private String episodeId;
    private String creatorId;
    private MediaProvider provider;
    private String providerPublicId;
    private String providerDeliveryType;
    private String uploadUniqueId;
    private String fileName;
    private Long fileSize;
    private String mimeType;
    private Long chunkSize;
    private Long uploadedBytes;
    private Integer totalChunks;
    private Integer lastUploadedChunkIndex;
    private MediaUploadSessionStatus status;
    private String errorMessage;
    private LocalDateTime expiredAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
