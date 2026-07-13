package com.talex.server.dtos.interaction.response;

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
public class SeriesRatingResponse {
    private UUID accountId;
    private String username;
    private String avatarUrl;
    private Double rate;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}