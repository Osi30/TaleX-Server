package com.talex.server.dtos.requests;

import com.talex.server.enums.MediaStatus;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaUpdateRequestDto {
    private Integer width;

    private Integer height;

    private String resolution;

    private Long duration;

    private Integer displayOrder;

    private MediaStatus status;

    private String actorId;
}
