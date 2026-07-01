package com.talex.server.dtos.responses.interaction;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private String id;
    private UUID accountId;
    private String episodeId;
    private String parentId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}