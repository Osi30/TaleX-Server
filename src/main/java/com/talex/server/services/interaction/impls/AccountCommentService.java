package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.interaction.request.CommentUpdateRequest;
import com.talex.server.dtos.interaction.response.CommentResponse;
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
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
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
        if (!ValidationUtils.isNullOrEmpty(request.getCommentParentId())) {
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
    public CommentResponse updateComment(UUID accountId, String commentId, CommentUpdateRequest request) {
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
    }

    @Override
    @Transactional
    public void hideCommentByAdmin(String commentId) {
        int affectedRows = commentRepository.hideCommentByAdmin(commentId);
        if (affectedRows == 0) {
            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR,
                    "Bình luận không tồn tại hoặc đã bị ẩn từ trước");
        }
    }

    @Transactional(readOnly = true)
    public Slice<CommentResponse> getTopLevelComments(String episodeId, Pageable pageable) {
        Slice<AccountComment> commentSlice = commentRepository.findTopLevelComments(episodeId, pageable);

        return commentSlice.map(comment -> mapToResponse(
                comment,
                comment.getAccount().getUsername(),
                comment.getAccount().getAvatarUrl()
        ));
    }

    @Transactional(readOnly = true)
    public Slice<CommentResponse> getCommentReplies(String parentCommentId, Pageable pageable) {
        Slice<AccountComment> replySlice = commentRepository.findRepliesByParentId(parentCommentId, pageable);

        return replySlice.map(comment -> mapToResponse(
                comment,
                comment.getAccount().getUsername(),
                comment.getAccount().getAvatarUrl()
        ));
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
                .repliesCount(comment.getReplies().size())
                .build();
    }
}