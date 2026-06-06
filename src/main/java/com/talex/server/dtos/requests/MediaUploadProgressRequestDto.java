package com.talex.server.dtos.requests;

import com.talex.server.enums.MediaUploadSessionStatus;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUploadProgressRequestDto {
    @NotNull
    @PositiveOrZero
    private Long uploadedBytes;

    private Integer lastUploadedChunkIndex;

    private MediaUploadSessionStatus status;

    private String actorId;
}
