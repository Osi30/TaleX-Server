package com.talex.server.dtos.requests;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MediaComicPageRequestDto {
    private String fileUrl;

    private Integer displayOrder;

    private Integer width;

    private Integer height;

    private String resolution;
}
