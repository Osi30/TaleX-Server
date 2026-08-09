package com.talex.server.dtos.responses.media;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaSystemConfigResponseDto {
    private UUID configId;
    private Integer maxComicImages;
    private Double maxComicImageSizeMb;
    private Double maxVideoSizeMb;
}
