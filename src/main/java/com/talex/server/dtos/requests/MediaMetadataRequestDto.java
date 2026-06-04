package com.talex.server.dtos.requests;

import com.talex.server.enums.MediaType;
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

    private Integer width;

    private Integer height;

    private String resolution;

    private Long duration;

    private Integer displayOrder;

    private String actorId;
}
