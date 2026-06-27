package com.talex.server.dtos.requests;

import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
import com.talex.server.enums.EpisodeUnlockType;
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

    private ContentType contentType;

    private EpisodeStatus status;

    private EpisodeUnlockType unlockType;

    private Long priceVnd;

    private Integer totalPage;

    private String actorId;
}
