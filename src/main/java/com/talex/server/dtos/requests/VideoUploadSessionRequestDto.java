package com.talex.server.dtos.requests;

import com.talex.server.enums.media.MediaProtectionType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class VideoUploadSessionRequestDto {
    @NotBlank
    private String fileName;

    @NotNull
    @Positive
    private Long fileSize;

    @NotBlank
    private String mimeType;

    private Long chunkSize;

    private MediaProtectionType protectionType;

    private String creatorId;

    private String actorId;
}
