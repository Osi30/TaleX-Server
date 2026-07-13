package com.talex.server.dtos.requests;

import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeRequestDto {
    private Integer episodeNumber;

    @NotBlank
    private String title;

    private String description;

    private String thumbnail;

    private ContentType contentType;

    private EpisodeStatus status;

    private Integer totalPage;

}
