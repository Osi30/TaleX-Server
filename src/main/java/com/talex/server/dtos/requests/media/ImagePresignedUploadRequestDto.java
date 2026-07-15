package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ImagePresignedUploadRequestDto {

    @NotBlank
    private String fileName;

    /** Must be an image MIME type, e.g. "image/jpeg", "image/png", "image/webp" */
    @NotBlank
    @Pattern(regexp = "image/.+", message = "mimeType must be an image/* MIME type")
    private String mimeType;

    @NotNull
    @Positive
    private Long fileSize;

    /**
     * Context for organizing S3 keys: "cover", "banner", "comic-page", "avatar".
     * Determines the sub-prefix under images/{env}/
     */
    @NotBlank
    private String imageContext;

    /** Optional: episodeId for comic pages, seriesId for covers/banners, etc. */
    private String entityId;

    /** Optional: creator or account ID performing the upload */
    private String actorId;
}
