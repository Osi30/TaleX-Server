package com.talex.server.dtos.responses.interaction;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class CommentResponse {
    private String commentId;
    private String content;
    private UUID accountId;
    private String username;
    private String avatarUrl;
    private String episodeId;
    private String parentCommentId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Integer repliesCount;
}