package com.talex.server.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaComicPageRequestDto {
    @NotBlank
    private String fileUrl;

    @NotNull
    @Positive
    private Integer displayOrder;

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
}
