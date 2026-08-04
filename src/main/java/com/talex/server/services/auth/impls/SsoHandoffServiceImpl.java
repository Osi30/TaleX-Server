package com.talex.server.services.auth.impls;

import com.talex.server.dtos.responses.auth.AuthResponse;
import com.talex.server.dtos.responses.auth.SsoHandoffResponse;
import com.talex.server.entities.auth.Account;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.auth.AuthService;
import com.talex.server.services.auth.SsoHandoffService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class SsoHandoffServiceImpl implements SsoHandoffService {

    private final StringRedisTemplate redisTemplate;
    private final AccountRepository accountRepository;
    private final AuthService authService;

    private static final String CODE_PREFIX = "sso_handoff:";
    private static final String RATE_PREFIX = "sso_handoff_rate:";
    private static final Duration CODE_TTL = Duration.ofSeconds(60);
    private static final Duration RATE_WINDOW = Duration.ofMinutes(1);
    private static final int MAX_CODES_PER_WINDOW = 10;

    @Override
    public SsoHandoffResponse createCode(UUID accountId) {
        enforceRateLimit(accountId);

        String code = UUID.randomUUID().toString();
        redisTemplate.opsForValue().set(CODE_PREFIX + code, accountId.toString(), CODE_TTL);

        log.info("SSO handoff code issued for accountId: {}", accountId);
        return SsoHandoffResponse.builder()
                .code(code)
                .expiresIn(CODE_TTL.getSeconds())
                .build();
    }

    @Override
    public AuthResponse exchange(String code) {
        String accountIdValue = redisTemplate.opsForValue().getAndDelete(CODE_PREFIX + code);
        if (accountIdValue == null) {
            throw new AuthException(AuthErrorCode.SSO_HANDOFF_INVALID);
        }

        Account account = accountRepository.findById(UUID.fromString(accountIdValue))
                .orElseThrow(() -> new AuthException(AuthErrorCode.SSO_HANDOFF_INVALID));

        log.info("SSO handoff code exchanged for accountId: {}", account.getAccountId());
        return authService.issueTokensFor(account);
    }

    private void enforceRateLimit(UUID accountId) {
        String key = RATE_PREFIX + accountId;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, RATE_WINDOW);
        }
        if (count != null && count > MAX_CODES_PER_WINDOW) {
            throw new AuthException(AuthErrorCode.SSO_HANDOFF_RATE_LIMITED);
        }
    }
}
