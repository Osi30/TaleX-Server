package com.talex.server.dtos.requests.media;

import com.talex.server.enums.media.MediaStatus;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaStatusRequestDto {
    @NotNull
    private MediaStatus status;

    private String actorId;
}
