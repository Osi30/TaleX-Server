package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.FollowRequestDto;
import com.talex.server.dtos.interaction.response.AccountFollowInfoDto;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.creator.CreatorLogRepository;
import com.talex.server.repositories.interaction.AccountFollowRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountFollowServiceImpl Tests")
class AccountFollowServiceImplTest {

    @Mock
    private AccountFollowRepository accountFollowRepository;
    @Mock
    private CreatorLogRepository creatorLogRepository;

    @InjectMocks
    private AccountFollowServiceImpl service;

    private UUID followerId;
    private UUID followedId;

    @BeforeEach
    void setUp() {
        followerId = UUID.randomUUID();
        followedId = UUID.randomUUID();
    }

    @Test
    @DisplayName("follow - Self Follow, Success, and DataIntegrityViolation branches")
    void follow() {
        // Case 1: Self Follow
        FollowRequestDto selfReq = new FollowRequestDto();
        selfReq.setFollowerId(followerId);
        selfReq.setFollowedId(followerId);

        assertThatThrownBy(() -> service.follow(selfReq))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.FOLLOW_SELF_NOT_ALLOWED);

        // Case 2: Success Follow
        FollowRequestDto validReq = new FollowRequestDto();
        validReq.setFollowerId(followerId);
        validReq.setFollowedId(followedId);

        when(accountFollowRepository.insertFollowNative(eq(followerId), eq(followedId), any())).thenReturn(1);
        service.follow(validReq);
        verify(creatorLogRepository).upsertCreatorLogFollows(eq(followedId), any(), eq(1L));

        // Case 3: DataIntegrityViolationException - Unique / Duplicate
        doThrow(new DataIntegrityViolationException("violates unique constraint account_follow_pkey"))
                .when(accountFollowRepository).insertFollowNative(any(), any(), any());
        assertThatThrownBy(() -> service.follow(validReq))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.FOLLOW_ALREADY_EXISTS);

        // Case 4: DataIntegrityViolationException - Foreign Key failure
        doThrow(new DataIntegrityViolationException("violates foreign key constraint fk_user"))
                .when(accountFollowRepository).insertFollowNative(any(), any(), any());
        assertThatThrownBy(() -> service.follow(validReq))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.ACCOUNT_NOT_FOUND);

        // Case 5: DataIntegrityViolationException - Other error
        doThrow(new DataIntegrityViolationException("generic db error"))
                .when(accountFollowRepository).insertFollowNative(any(), any(), any());
        assertThatThrownBy(() -> service.follow(validReq))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.SAVING_DATABASE_ERROR);
    }

    @Test
    @DisplayName("unfollow - Success vs Not Found")
    void unfollow() {
        FollowRequestDto req = new FollowRequestDto();
        req.setFollowerId(followerId);
        req.setFollowedId(followedId);

        // Success
        when(accountFollowRepository.deleteByFollowerIdAndFollowedId(followerId, followedId)).thenReturn(1);
        service.unfollow(req);
        verify(creatorLogRepository).upsertCreatorLogFollows(eq(followedId), any(), eq(-1L));

        // Not Found (affectedRows == 0)
        when(accountFollowRepository.deleteByFollowerIdAndFollowedId(followerId, followedId)).thenReturn(0);
        assertThatThrownBy(() -> service.unfollow(req))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.FOLLOW_NOT_FOUND);
    }

    @Test
    @DisplayName("getFollowers & getFollowed - Slice retrieval")
    void getFollowersAndFollowed() {
        Pageable pageable = PageRequest.of(0, 10);
        AccountFollowInfoDto info = new AccountFollowInfoDto(followerId, "follower", "avatar.jpg", LocalDateTime.now());
        Slice<AccountFollowInfoDto> slice = new SliceImpl<>(List.of(info), pageable, false);

        when(accountFollowRepository.findFollowersByAccountId(followedId, pageable)).thenReturn(slice);
        Slice<AccountFollowInfoDto> followers = service.getFollowers(followedId, pageable);
        assertThat(followers.getContent()).hasSize(1);

        when(accountFollowRepository.findFollowedByAccountId(followerId, pageable)).thenReturn(slice);
        Slice<AccountFollowInfoDto> followed = service.getFollowed(followerId, pageable);
        assertThat(followed.getContent()).hasSize(1);
    }
}
