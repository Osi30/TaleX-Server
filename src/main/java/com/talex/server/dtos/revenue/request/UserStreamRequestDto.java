package com.talex.server.dtos.revenue.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserStreamRequestDto {
    private String userId;
    private Long totalStreams;
    private Map<String, Long> artistStreams;
    private Map<String, Map<String, Long>> artistEpisodeStreams;
}