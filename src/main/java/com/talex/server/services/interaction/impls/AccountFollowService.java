package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.response.AccountFollowInfoDto;
import com.talex.server.dtos.interaction.request.FollowRequestDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.interaction.AccountFollow;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.interaction.AccountFollowRepository;
import com.talex.server.services.interaction.IAccountFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountFollowService implements IAccountFollowService {
    private final AccountFollowRepository accountFollowRepository;
    private final AccountRepository accountRepository;

    @Override
    @Transactional
    public void follow(FollowRequestDto request) {
        UUID followerId = request.getFollowerId();
        UUID followedId = request.getFollowedId();

        // Đăng kí trùng
        if (followerId.equals(followedId)) {
            throw new InteractionException(InteractionErrorCode.FOLLOW_SELF_NOT_ALLOWED);
        }

        try {
            Account followerRef = accountRepository.getReferenceById(followerId);
            Account followedRef = accountRepository.getReferenceById(followedId);

            AccountFollow follow = AccountFollow.builder()
                    .follower(followerRef)
                    .followed(followedRef)
                    .build();
            accountFollowRepository.saveAndFlush(follow);

        } catch (DataIntegrityViolationException ex) {
            String dbErrorMessage = ex.getMostSpecificCause().getMessage();
            if (dbErrorMessage == null) {
                throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR);
            }

            String lowerMessage = dbErrorMessage.toLowerCase();

            // Đã tồn tại cặp follow này rồi
            if (lowerMessage.contains("duplicate key") || lowerMessage.contains("unique constraint") || lowerMessage.contains("account_follow_pkey")) {
                throw new InteractionException(InteractionErrorCode.FOLLOW_ALREADY_EXISTS);
            }

            // Một hoặc cả hai account_id không tồn tại thực tế trong bảng accounts
            if (lowerMessage.contains("foreign key") || lowerMessage.contains("violates fk") || lowerMessage.contains("fk_")) {
                throw new InteractionException(InteractionErrorCode.ACCOUNT_NOT_FOUND);
            }

            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR, "Lỗi hệ thống khi lưu tương tác theo dõi.");
        }
    }

    @Override
    @Transactional
    public void unfollow(FollowRequestDto request) {
        int affectedRows = accountFollowRepository.deleteByFollowerIdAndFollowedId(
                request.getFollowerId(), request.getFollowedId());
        if (affectedRows == 0) {
            throw new InteractionException(InteractionErrorCode.FOLLOW_NOT_FOUND);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AccountFollowInfoDto> getFollowers(UUID accountId, Pageable pageable) {
        return accountFollowRepository.findFollowersByAccountId(accountId, pageable);
    }

    @Override
    @Transactional(readOnly = true)
    public Slice<AccountFollowInfoDto> getFollowed(UUID accountId, Pageable pageable) {
        return accountFollowRepository.findFollowedByAccountId(accountId, pageable);
    }
}