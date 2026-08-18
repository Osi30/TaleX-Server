package com.talex.server.dtos.responses.series;

import com.talex.server.entities.analytic.AnalyticData;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.SeriesStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesResponseDto {
    private String seriesId;
    private String accountId;
    private String creatorId;
    private String creatorName;
    private String creatorAvatar;
    private Long totalCreatorFollowers;
    private String title;
    private String description;
    private String coverUrl;
    private String bannerUrl;
    private ContentType contentType;
    private SeriesStatus status;
    private String ageRating;
    private Set<String> contentWarnings;
    private String language;
    private AnalyticData analyticData = new AnalyticData();
    private Double averageRating = 0.0;
    private List<CategoryResponseDto> categories;
    private List<TagResponseDto> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
}
