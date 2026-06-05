package com.talex.server.dtos.responses;

import com.talex.server.enums.MediaProvider;
import com.talex.server.enums.MediaUploadSessionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadSessionResponseDto {
    private String uploadSessionId;
    private String mediaId;
    private String episodeId;
    private MediaProvider provider;
    private String cloudName;
    private String apiKey;
    private Long timestamp;
    private String signature;
    private String publicId;
    private String resourceType;
    private String uploadUrl;
    private String uploadUniqueId;
    private Long chunkSize;
    private Long fileSize;
    private String fileName;
    private String mimeType;
    private String providerDeliveryType;
    private Map<String, String> uploadParams;
    private Long uploadedBytes;
    private Integer lastUploadedChunkIndex;
    private MediaUploadSessionStatus status;
    private LocalDateTime expiredAt;
}
