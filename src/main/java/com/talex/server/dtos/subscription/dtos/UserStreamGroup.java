package com.talex.server.dtos.subscription.dtos;

import com.talex.server.dtos.revenue.request.UserStreamRequestDto;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class UserStreamGroup {
    private final Map<String, UserStreamAccumulator> userMap = new LinkedHashMap<>();

    public void addStat(String userId, String creatorId, String episodeId, Long views) {
        userMap.computeIfAbsent(userId, UserStreamAccumulator::new)
                .addStream(creatorId, episodeId, views);
    }

    public List<UserStreamRequestDto> toUserStreamRequestDtos() {
        return userMap.values().stream()
                .map(UserStreamAccumulator::toDto)
                .toList();
    }
}
