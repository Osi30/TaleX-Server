package com.talex.server.dtos.responses.series;

import com.talex.server.enums.series.EpisodeStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComboEpisodeResponseDto {
    private String comboId;
    private String creatorId;
    private String title;
    private String description;
    private EpisodeStatus status;
    private Long priceVnd;
    private Long originalPriceVnd;
    private List<EpisodeSummaryDto> episodes; // Reusing or defining a basic summary, we'll define a simple one here if needed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class EpisodeSummaryDto {
        private String episodeId;
        private String title;
        private String thumbnail;
        private Integer episodeNumber;
        private Long priceVnd;
        private String seasonId;
        private String seasonTitle;
        private String seriesId;
        private String seriesTitle;
    }
}
