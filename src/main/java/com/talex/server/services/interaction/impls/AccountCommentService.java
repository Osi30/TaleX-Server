package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.requests.interaction.CommentRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;
import com.talex.server.entities.interaction.AccountComment;
import com.talex.server.repositories.interaction.AccountCommentRepository;
import com.talex.server.services.interaction.IAccountCommentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountCommentService implements IAccountCommentService {
    private final AccountCommentRepository commentRepository;

    @Override
    @Transactional
    public CommentResponse createComment(String episodeId, CommentRequest request) {
        AccountComment accountComment = AccountComment.builder()
//                .account(account)
//                .episode(episode)
                .content(request.getContent())
                .isDeleted(false)
                .build();

        // Nếu có truyền parentId -> Tìm comment cha gắn vào
        if (request.getCommentParentId() != null) {
            AccountComment parent = commentRepository.findByIdAndIsDeletedFalse(request.getCommentParentId())
                    .orElseThrow(() -> new RuntimeException("Không tìm thấy bình luận gốc để phản hồi"));
            accountComment.setParentComment(parent);
        }

        AccountComment saved = commentRepository.save(accountComment);
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(String commentId, CommentRequest request) {
        AccountComment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại hoặc đã bị xóa"));
        comment.setContent(request.getContent());
        return mapToResponse(commentRepository.save(comment));
    }

    @Override
    @Transactional
    public void deleteComment(String commentId) {
        AccountComment comment = commentRepository.findByIdAndIsDeletedFalse(commentId)
                .orElseThrow(() -> new RuntimeException("Bình luận không tồn tại hoặc đã bị xóa"));
        comment.setIsDeleted(true);
        commentRepository.save(comment);
    }

    private CommentResponse mapToResponse(AccountComment entity) {
        return CommentResponse.builder()
                .id(entity.getId())
                .accountId(entity.getAccount().getAccountId())
                .episodeId(entity.getEpisode().getEpisodeId())
                .parentId(entity.getParentComment() != null ? entity.getParentComment().getId() : null)
                .content(entity.getContent())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}