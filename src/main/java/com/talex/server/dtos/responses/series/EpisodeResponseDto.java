package com.talex.server.dtos.responses.series;

import com.talex.server.entities.analytic.AnalyticData;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.enums.series.EpisodeUnlockType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EpisodeResponseDto {
    private String episodeId;
    private String seasonId;
    // Cần cho FE dựng URL điều hướng sâu (vd từ thông báo "episode bị ẩn" -> đi thẳng tới
    // đúng episode) — route dashboard yêu cầu đủ seriesId+seasonId+episodeId trên query string.
    private String seriesId;
    private String creatorId;
    private Integer episodeNumber;
    private String title;
    private String description;
    private String thumbnail;
    private ContentType contentType;
    private EpisodeStatus status;
    private LocalDateTime scheduledPublishAt;
    private LocalDateTime publishedAt;
    private EpisodeUnlockType unlockType;
    private Long priceVnd;
    private AnalyticData analyticData = new AnalyticData();
    private Integer totalPage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
}
