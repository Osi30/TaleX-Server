package com.talex.server.dtos.interaction.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AccountLikeResponse {
    private String episodeId;
    private String episodeTitle;
    private Integer episodeNumber;
    private String seriesTitle;
    private String seriesCoverUrl;
    private LocalDateTime likedAt;
}