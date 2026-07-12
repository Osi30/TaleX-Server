package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.FollowRequestDto;
import com.talex.server.dtos.interaction.response.AccountFollowInfoDto;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.creator.CreatorLogRepository;
import com.talex.server.repositories.interaction.AccountFollowRepository;
import com.talex.server.services.interaction.IAccountFollowService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountFollowService implements IAccountFollowService {
    private final AccountFollowRepository accountFollowRepository;
    private final CreatorLogRepository creatorLogRepository;

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
            LocalDateTime now = LocalDateTime.now();
            int affectedRows = accountFollowRepository.insertFollowNative(followerId, followedId, now);

            if (affectedRows > 0) {
                // 2. Làm tròn thời gian đến giờ hiện tại phục vụ hour_bucket
                LocalDateTime hourBucket = now.withMinute(0).withSecond(0).withNano(0);

                // 3. Tiến hành Upsert +1 follow cho Creator Log
                creatorLogRepository.upsertCreatorLogFollows(followedId, hourBucket, 1L);
            }

        } catch (DataIntegrityViolationException ex) {
            String lowerMessage = ex.getMessage() != null ? ex.getMessage().toLowerCase() : "";

            // Trùng khóa chính hoặc vi phạm UNIQUE (Đã từng follow từ trước)
            if (lowerMessage.contains("unique") || lowerMessage.contains("constraint") || lowerMessage.contains("account_follow_pkey")) {
                throw new InteractionException(InteractionErrorCode.FOLLOW_ALREADY_EXISTS);
            }

            // Lỗi liên kết khóa ngoại (Tài khoản follower hoặc followed không có thực)
            if (lowerMessage.contains("foreign key") || lowerMessage.contains("violates fk") || lowerMessage.contains("fk_")) {
                throw new InteractionException(InteractionErrorCode.ACCOUNT_NOT_FOUND);
            }

            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR, "Lỗi hệ thống database khi lưu tương tác follow.");
        }
    }

    @Override
    @Transactional
    public void unfollow(FollowRequestDto request) {
        UUID followerId = request.getFollowerId();
        UUID followedId = request.getFollowedId();

        int affectedRows = accountFollowRepository.deleteByFollowerIdAndFollowedId(
                followerId, followedId);
        if (affectedRows == 0) {
            throw new InteractionException(InteractionErrorCode.FOLLOW_NOT_FOUND);
        }
        LocalDateTime hourBucket = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);
        creatorLogRepository.upsertCreatorLogFollows(followedId, hourBucket, -1L);
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