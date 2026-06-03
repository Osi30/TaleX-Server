package com.talex.server.dtos.responses;

import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
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
    private Integer episodeNumber;
    private String title;
    private String description;
    private ContentType contentType;
    private EpisodeStatus status;
    private LocalDateTime publishedAt;
    private Long likes;
    private Long views;
    private Integer totalPage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;
    private String createdBy;
    private String updatedBy;
    private String deletedBy;
    private Boolean isDeleted;
}
