package com.talex.server.dtos.requests.media;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaSystemConfigRequestDto {

    @NotNull
    @Positive
    private Integer maxComicImages;

    @NotNull
    @Positive
    private Double maxComicImageSizeMb;

    @NotNull
    @Positive
    private Double maxVideoSizeMb;
}
