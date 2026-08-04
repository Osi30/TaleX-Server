package com.talex.server.dtos.interaction.response;

import com.talex.server.dtos.responses.series.EpisodeResponseDto;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WatchSessionResponseDto {
    private String id;
    private EpisodeResponseDto episode;
    private Double watchDuration;
    private Integer heartbeatCount;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Double currentPosition;
    private LocalDateTime updatedAt;
}