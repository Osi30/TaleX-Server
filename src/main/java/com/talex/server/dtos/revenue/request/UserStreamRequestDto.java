package com.talex.server.dtos.revenue.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
    @JsonIgnore
    private Long totalStreams;
    @JsonIgnore
    private Map<String, Long> artistStreams;
    private Map<String, Map<String, Long>> artistEpisodeStreams;
}