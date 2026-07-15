package com.talex.server.dtos.responses.series;

import com.talex.server.enums.series.SeasonStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeasonResponseDto {
    private String seasonId;
    private String seriesId;
    private String creatorId;
    private Integer seasonNumber;
    private String title;
    private String description;
    private SeasonStatus status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private Boolean isDeleted;
}
