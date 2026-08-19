package com.talex.server.dtos.subscription.dtos;

import com.talex.server.dtos.revenue.request.UserStreamRequestDto;

import java.util.LinkedHashMap;
import java.util.Map;

public class UserStreamAccumulator {
    private final String userId;
    private final Map<String, Map<String, Long>> artistEpisodeStreams = new LinkedHashMap<>();

    public UserStreamAccumulator(String userId) {
        this.userId = userId;
    }

    public void addStream(String creatorId, String episodeId, Long views) {
        artistEpisodeStreams.computeIfAbsent(creatorId, k -> new LinkedHashMap<>())
                .merge(episodeId, views != null ? views : 0L, Long::sum);
    }

    public UserStreamRequestDto toDto() {
        return UserStreamRequestDto.builder()
                .userId(userId)
                .artistEpisodeStreams(artistEpisodeStreams)
                .build();
    }
}
