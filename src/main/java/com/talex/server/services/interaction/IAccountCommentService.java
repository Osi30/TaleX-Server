package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;

import java.util.UUID;

public interface IAccountCommentService {
    CommentResponse createComment(UUID accountId, CommentRequest request);

    CommentResponse updateComment(UUID accountId, String commentId, CommentRequest request);

    void deleteCommentByOwner(UUID accountId, String commentId);

    // Admin/Staff Only
    void hideCommentByAdmin(String commentId, boolean isHide);
}
