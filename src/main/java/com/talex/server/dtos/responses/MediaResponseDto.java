package com.talex.server.dtos.responses;

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
    private String cloudinaryPublicId;
    private String storageProvider;
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
