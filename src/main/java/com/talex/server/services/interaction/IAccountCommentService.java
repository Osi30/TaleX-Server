package com.talex.server.services.interaction;

import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.interaction.request.CommentUpdateRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;

import java.util.UUID;

public interface IAccountCommentService {
    CommentResponse createComment(UUID accountId, CommentRequest request);

    CommentResponse updateComment(UUID accountId, String commentId, CommentUpdateRequest request);

    Slice<CommentResponse> getTopLevelComments(String episodeId, Pageable pageable);

    Slice<CommentResponse> getCommentReplies(String parentCommentId, Pageable pageable);

    void deleteCommentByOwner(UUID accountId, String commentId);

    // Admin/Staff Only
    void hideCommentByAdmin(String commentId);
}
