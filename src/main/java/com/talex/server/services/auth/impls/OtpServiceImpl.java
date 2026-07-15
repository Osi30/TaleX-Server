package com.talex.server.services.auth.impls;

import com.talex.server.entities.auth.Account;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.services.auth.EmailService;
import com.talex.server.services.auth.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final StringRedisTemplate redisTemplate;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();
    private static final String OTP_KEY_PREFIX = "otp:";
    private static final String RESEND_COOLDOWN_PREFIX = "resend_cooldown:";
    private static final String PWD_RESET_KEY_PREFIX = "pwd_reset:";
    private static final String PWD_RESET_COOLDOWN_PREFIX = "pwd_reset_cooldown:";

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    @Value("${otp.resend-cooldown-minutes:2}")
    private int resendCooldownMinutes;

    @Async("emailExecutor")
    @Override
    public void generateAndSend(Account account) {
        String code = generateOtpCode();
        String key = OTP_KEY_PREFIX + account.getAccountId();

        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(otpExpirationMinutes));
        emailService.sendOtpEmailAsync(account.getEmail(), code);
        log.info("OTP generated for accountId: {}", account.getAccountId());
    }

    @Override
    public void verify(UUID accountId, String code) {
        String key = OTP_KEY_PREFIX + accountId;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null || !storedCode.equals(code)) {
            log.warn("OTP verify failed for accountId: {}", accountId);
            throw new AuthException(AuthErrorCode.INVALID_OTP);
        }

        redisTemplate.delete(key);
    }

    @Override
    public void enforceResendCooldown(UUID accountId) {
        String key = RESEND_COOLDOWN_PREFIX + accountId;
        Boolean wasAbsent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(resendCooldownMinutes));

        if (Boolean.FALSE.equals(wasAbsent)) {
            throw new AuthException(AuthErrorCode.OTP_RATE_LIMITED);
        }
    }

    @Async("emailExecutor")
    @Override
    public void generateAndSendPasswordReset(Account account) {
        String code = generateOtpCode();
        String key = PWD_RESET_KEY_PREFIX + account.getAccountId();

        redisTemplate.opsForValue().set(key, code, Duration.ofMinutes(otpExpirationMinutes));
        emailService.sendPasswordResetEmailAsync(account.getEmail(), code);
        log.info("Password reset OTP generated for accountId: {}", account.getAccountId());
    }

    @Override
    public void verifyPasswordReset(UUID accountId, String code) {
        String key = PWD_RESET_KEY_PREFIX + accountId;
        String storedCode = redisTemplate.opsForValue().get(key);

        if (storedCode == null || !storedCode.equals(code)) {
            log.warn("Password reset OTP verify failed for accountId: {}", accountId);
            throw new AuthException(AuthErrorCode.INVALID_OTP);
        }

        redisTemplate.delete(key);
    }

    @Override
    public void enforcePasswordResetCooldown(UUID accountId) {
        String key = PWD_RESET_COOLDOWN_PREFIX + accountId;
        Boolean wasAbsent = redisTemplate.opsForValue()
                .setIfAbsent(key, "1", Duration.ofMinutes(resendCooldownMinutes));

        if (Boolean.FALSE.equals(wasAbsent)) {
            throw new AuthException(AuthErrorCode.OTP_RATE_LIMITED);
        }
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        String format = "%0" + otpLength + "d";
        return String.format(format, RANDOM.nextInt(bound));
    }
}
