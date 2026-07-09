package com.talex.server.dtos.interaction.response;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class EpisodeLikeResponse {
    private UUID accountId;
    private String username;
    private String avatarUrl;
    private LocalDateTime likedAt;
}