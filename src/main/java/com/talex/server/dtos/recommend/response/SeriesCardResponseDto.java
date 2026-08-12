package com.talex.server.dtos.recommend;

import com.talex.server.enums.series.ContentType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesCardResponseDto {
    private String seriesId;
    private UUID accountId;
    private String creatorId;
    private String creatorName;
    private String creatorAvatar;
    private Long totalCreatorFollowers;
    private String title;
    private String description;
    private String coverUrl;
    private String bannerUrl;
    private ContentType contentType;
    private String ageRating;
    private String language;
    private Long totalViews;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Double averageRating;
    private LocalDateTime releasedUpdateTime;
}
