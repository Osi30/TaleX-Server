package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.responses.interaction.CommentResponse;
import com.talex.server.entities.Account;
import com.talex.server.entities.interaction.AccountComment;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.interaction.AccountCommentRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.services.interaction.IAccountCommentService;
import com.talex.server.utils.ValidationUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountCommentService implements IAccountCommentService {
    private final AccountCommentRepository commentRepository;
    private final EpisodeRepository episodeRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public CommentResponse createComment(UUID accountId, CommentRequest request) {
        Account accountProxy = accountRepository.getReferenceById(accountId);
        Episode episodeProxy = episodeRepository.getReferenceById(request.getEpisodeId());

        AccountComment parentProxy = null;
        if (ValidationUtils.isNullOrEmpty(request.getCommentParentId())) {
            parentProxy = commentRepository.getReferenceById(request.getCommentParentId());
        }

        AccountComment comment = AccountComment.builder()
                .content(request.getContent())
                .account(accountProxy)
                .episode(episodeProxy)
                .parentComment(parentProxy)
                .build();

        AccountComment savedComment = commentRepository.save(comment);
        return mapToResponse(savedComment, null, null);
    }

    @Override
    @Transactional
    public CommentResponse updateComment(UUID accountId, String commentId, CommentRequest request) {
        AccountComment comment = commentRepository.findByIdAndAccountIdForUpdate(commentId, accountId)
                .orElseThrow(() -> new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR,
                        "Bình luận không tồn tại, đã bị khóa hoặc bạn không có quyền chỉnh sửa"));

        if (ValidationUtils.isNullOrEmpty(request.getContent())) {
            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR, "Nội dung chỉnh sửa không được để trống");
        }
        comment.setContent(request.getContent());
        AccountComment updatedComment = commentRepository.save(comment);
        return mapToResponse(updatedComment, comment.getAccount().getUsername(), comment.getAccount().getAvatarUrl());
    }

    @Override
    @Transactional
    public void deleteCommentByOwner(UUID accountId, String commentId) {
        int affectedRows = commentRepository.deleteByCommentIdAndAccountId(commentId, accountId);
        if (affectedRows == 0) {
            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR,
                    "Không thể xóa! Bình luận không tồn tại hoặc bạn không phải chủ sở hữu");
        }
        // KHÔNG GỌI LOG THỦ CÔNG: CDC Debezium sẽ tự bắt sự kiện DELETE này bắn qua Kafka!
    }

    @Override
    @Transactional
    public void hideCommentByAdmin(String commentId, boolean isHide) {
        int affectedRows = commentRepository.hideCommentByAdmin(commentId, isHide);
        if (affectedRows == 0) {
            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR,
                    "Bình luận không tồn tại hoặc đã bị ẩn từ trước");
        }
    }

    private CommentResponse mapToResponse(AccountComment comment, String fallbackUsername, String fallbackAvatar) {
        return CommentResponse.builder()
                .commentId(comment.getCommentId())
                .content(comment.getContent())
                .accountId(comment.getAccount().getAccountId())
                .username(fallbackUsername != null ? fallbackUsername : "User")
                .avatarUrl(fallbackAvatar)
                .episodeId(comment.getEpisode().getEpisodeId())
                .parentCommentId(comment.getParentComment() != null ? comment.getParentComment().getCommentId() : null)
                .build();
    }
}