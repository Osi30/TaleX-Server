package com.talex.server.services.impls;

import com.talex.server.entities.Account;
import com.talex.server.entities.RefreshToken;
import com.talex.server.exceptions.UnauthorizedException;
import com.talex.server.repositories.RefreshTokenRepository;
import com.talex.server.services.RefreshTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Override
    public String createRefreshToken(Account account) {
        RefreshToken refreshToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .account(account)
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(refreshToken);
        log.info("Refresh token created for: {}", account.getAccountId());
        return refreshToken.getToken();
    }

    @Override
    public RefreshToken findValidToken(String token) {
        RefreshToken existing = refreshTokenRepository.findByTokenAndRevokedFalse(token)
                .orElseThrow(() -> {
                    log.warn("Refresh token not found or revoked");
                    return new UnauthorizedException("Invalid refresh token");
                });

        if (existing.getExpiryDate().isBefore(LocalDateTime.now())) {
            existing.setRevoked(true);
            refreshTokenRepository.save(existing);
            log.warn("Refresh token expired");
            throw new UnauthorizedException("Refresh token expired");
        }

        return existing;
    }

    @Override
    @Transactional
    public RefreshToken validateAndRotate(String token) {
        RefreshToken existing = findValidToken(token);

        // Revoke old token
        existing.setRevoked(true);
        refreshTokenRepository.save(existing);

        // Create new token (rotation)
        RefreshToken newToken = RefreshToken.builder()
                .token(UUID.randomUUID().toString())
                .account(existing.getAccount())
                .expiryDate(LocalDateTime.now().plusSeconds(refreshTokenExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(newToken);
        log.info("Refresh token rotated for: {}", existing.getAccount().getAccountId());
        return newToken;
    }

    @Override
    @Transactional
    public void revokeAllByAccount(UUID accountId) {
        refreshTokenRepository.revokeAllByAccountId(accountId);
        log.info("All refresh tokens revoked for: {}", accountId);
    }
}
