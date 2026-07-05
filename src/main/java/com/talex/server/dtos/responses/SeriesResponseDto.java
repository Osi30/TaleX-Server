package com.talex.server.dtos.responses;

import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.SeriesStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesResponseDto {
    private String seriesId;
    private String creatorId;
    private String title;
    private String description;
    private String coverUrl;
    private String bannerUrl;
    private ContentType contentType;
    private SeriesStatus status;
    private String ageRating;
    private String language;
    private Long totalViews;
    private Long totalSubscriptions;
    private List<CategoryResponseDto> categories;
    private List<TagResponseDto> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
}
