package com.talex.server.services.interaction;

import com.talex.server.dtos.requests.interaction.CommentRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;

public interface IAccountCommentService {
    CommentResponse createComment(String episodeId, CommentRequest request);
    CommentResponse updateComment(String commentId, CommentRequest request);
    void deleteComment(String commentId);
}
