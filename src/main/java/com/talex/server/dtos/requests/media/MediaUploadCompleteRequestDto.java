package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadCompleteRequestDto {
    private String assetId;

    @NotBlank
    private String publicId;

    @NotBlank
    private String secureUrl;

    private String resourceType;

    private String format;

    @NotNull
    @PositiveOrZero
    private Long bytes;

    private Double duration;

    private Integer width;

    private Integer height;

    private String actorId;
}
