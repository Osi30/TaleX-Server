package com.talex.server.dtos.requests;

import com.talex.server.enums.MediaType;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaMetadataRequestDto {
    private MediaType mediaType;

    private Integer width;

    private Integer height;

    private String resolution;

    private Long duration;

    private Integer displayOrder;

    private String actorId;
}
