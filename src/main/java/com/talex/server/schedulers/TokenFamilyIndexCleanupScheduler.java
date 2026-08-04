package com.talex.server.schedulers;

import com.talex.server.services.auth.TokenFamilyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * Dọn rác định kỳ index `account_families:{accountId}` trên Redis — Set này không
 * có TTL (chỉ family HASH riêng mới tự hết hạn), nên nếu 1 account đăng nhập nhiều
 * lần (VD mobile app tự gọi lại /api/auth/google thay vì /refresh-token) mà không
 * bao giờ logout, familyId đã hết hạn vẫn tồn tại trong Set mãi mãi. Không ảnh
 * hưởng bảo mật (family HASH hết hạn không auth được), chỉ là resource hygiene.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TokenFamilyIndexCleanupScheduler {

    private final TokenFamilyService tokenFamilyService;

    @Scheduled(
            fixedDelayString = "${auth.family-cleanup-interval-ms:3600000}",
            initialDelayString = "${auth.family-cleanup-initial-delay-ms:3600000}")
    public void cleanupStaleFamilyIndexes() {
        try {
            tokenFamilyService.pruneStaleFamilyIndexes();
        } catch (RuntimeException exception) {
            log.warn("Failed to prune stale token family indexes", exception);
        }
    }
}
