package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.CommentRequest;
import com.talex.server.dtos.interaction.request.CommentUpdateRequest;
import com.talex.server.dtos.interaction.response.CommentResponse;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.interaction.AccountComment;
import com.talex.server.entities.series.Episode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.interaction.AccountCommentRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountCommentServiceImpl Tests")
class AccountCommentServiceImplTest {

    @Mock
    private AccountCommentRepository commentRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private AccountRepository accountRepository;

    @InjectMocks
    private AccountCommentServiceImpl service;

    private UUID accountId;
    private Account account;
    private Episode episode;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        account = new Account();
        account.setAccountId(accountId);
        account.setUsername("john_doe");
        account.setAvatarUrl("avatar.png");

        episode = new Episode();
        episode.setEpisodeId("ep-100");
    }

    @Test
    @DisplayName("createComment - Top-level vs reply to parent comment")
    void createComment() {
        when(accountRepository.getReferenceById(accountId)).thenReturn(account);
        when(episodeRepository.getReferenceById("ep-100")).thenReturn(episode);

        // Case 1: Top-level comment
        CommentRequest topLevelReq = new CommentRequest();
        topLevelReq.setEpisodeId("ep-100");
        topLevelReq.setContent("Great episode!");

        AccountComment savedTopLevel = AccountComment.builder()
                .commentId("c-1")
                .content("Great episode!")
                .account(account)
                .episode(episode)
                .replies(new ArrayList<>())
                .build();

        when(commentRepository.save(any(AccountComment.class))).thenReturn(savedTopLevel);

        CommentResponse res1 = service.createComment(accountId, topLevelReq);

        assertThat(res1.getCommentId()).isEqualTo("c-1");
        assertThat(res1.getContent()).isEqualTo("Great episode!");
        assertThat(res1.getParentCommentId()).isNull();

        // Case 2: Reply to parent comment
        CommentRequest replyReq = new CommentRequest();
        replyReq.setEpisodeId("ep-100");
        replyReq.setCommentParentId("c-1");
        replyReq.setContent("Agreed!");

        AccountComment parentComment = AccountComment.builder().commentId("c-1").build();
        when(commentRepository.getReferenceById("c-1")).thenReturn(parentComment);

        AccountComment savedReply = AccountComment.builder()
                .commentId("c-2")
                .content("Agreed!")
                .account(account)
                .episode(episode)
                .parentComment(parentComment)
                .replies(new ArrayList<>())
                .build();

        when(commentRepository.save(any(AccountComment.class))).thenReturn(savedReply);

        CommentResponse res2 = service.createComment(accountId, replyReq);

        assertThat(res2.getCommentId()).isEqualTo("c-2");
        assertThat(res2.getParentCommentId()).isEqualTo("c-1");
    }

    @Test
    @DisplayName("updateComment - Unauthorized/Absent, Blank Content, and Success Update")
    void updateComment() {
        CommentUpdateRequest updateReq = new CommentUpdateRequest();
        updateReq.setContent("New Content");

        // Case 1: Not found / Unauthorized
        when(commentRepository.findByIdAndAccountIdForUpdate("c-1", accountId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.updateComment(accountId, "c-1", updateReq))
                .isInstanceOf(InteractionException.class);

        // Case 2: Blank content
        AccountComment existing = AccountComment.builder()
                .commentId("c-1")
                .account(account)
                .episode(episode)
                .replies(new ArrayList<>())
                .build();
        when(commentRepository.findByIdAndAccountIdForUpdate("c-1", accountId)).thenReturn(Optional.of(existing));

        CommentUpdateRequest emptyReq = new CommentUpdateRequest();
        emptyReq.setContent("   ");
        assertThatThrownBy(() -> service.updateComment(accountId, "c-1", emptyReq))
                .isInstanceOf(InteractionException.class);

        // Case 3: Success update
        updateReq.setContent("Updated content");
        when(commentRepository.save(existing)).thenReturn(existing);

        CommentResponse res = service.updateComment(accountId, "c-1", updateReq);

        assertThat(res.getContent()).isEqualTo("Updated content");
        assertThat(res.getUsername()).isEqualTo("john_doe");
    }

    @Test
    @DisplayName("deleteCommentByOwner - Success vs Failure")
    void deleteCommentByOwner() {
        // Success
        when(commentRepository.deleteByCommentIdAndAccountId("c-1", accountId)).thenReturn(1);
        service.deleteCommentByOwner(accountId, "c-1");

        // Failure (affectedRows == 0)
        when(commentRepository.deleteByCommentIdAndAccountId("c-1", accountId)).thenReturn(0);
        assertThatThrownBy(() -> service.deleteCommentByOwner(accountId, "c-1"))
                .isInstanceOf(InteractionException.class);
    }

    @Test
    @DisplayName("hideCommentByAdmin - Success vs Failure")
    void hideCommentByAdmin() {
        // Success
        when(commentRepository.hideCommentByAdmin("c-1")).thenReturn(1);
        service.hideCommentByAdmin("c-1");

        // Failure (affectedRows == 0)
        when(commentRepository.hideCommentByAdmin("c-1")).thenReturn(0);
        assertThatThrownBy(() -> service.hideCommentByAdmin("c-1"))
                .isInstanceOf(InteractionException.class);
    }

    @Test
    @DisplayName("getTopLevelComments & getCommentReplies - Returns mapped Slice")
    void getSliceComments() {
        Pageable pageable = PageRequest.of(0, 10);
        AccountComment c = AccountComment.builder()
                .commentId("c-1")
                .content("Hello")
                .account(account)
                .episode(episode)
                .replies(new ArrayList<>())
                .build();

        Slice<AccountComment> slice = new SliceImpl<>(List.of(c), pageable, false);

        when(commentRepository.findTopLevelComments("ep-100", pageable)).thenReturn(slice);
        Slice<CommentResponse> topRes = service.getTopLevelComments("ep-100", pageable);
        assertThat(topRes.getContent()).hasSize(1);
        assertThat(topRes.getContent().get(0).getCommentId()).isEqualTo("c-1");

        when(commentRepository.findRepliesByParentId("c-parent", pageable)).thenReturn(slice);
        Slice<CommentResponse> replyRes = service.getCommentReplies("c-parent", pageable);
        assertThat(replyRes.getContent()).hasSize(1);
    }
}
