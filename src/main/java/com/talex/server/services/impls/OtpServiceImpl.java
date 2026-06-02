package com.talex.server.services.impls;

import com.talex.server.entities.Account;
import com.talex.server.entities.OtpVerification;
import com.talex.server.repositories.OtpVerificationRepository;
import com.talex.server.services.EmailService;
import com.talex.server.services.OtpService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class OtpServiceImpl implements OtpService {

    private final OtpVerificationRepository otpVerificationRepository;
    private final EmailService emailService;

    private static final SecureRandom RANDOM = new SecureRandom();

    @Value("${otp.expiration-minutes}")
    private int otpExpirationMinutes;

    @Value("${otp.length}")
    private int otpLength;

    @Override
    @Transactional
    public void generateAndSend(Account account) {
        otpVerificationRepository.invalidatePreviousOtps(account.getAccountId());

        String code = generateOtpCode();

        OtpVerification otp = OtpVerification.builder()
                .account(account)
                .code(code)
                .expiresAt(LocalDateTime.now().plusMinutes(otpExpirationMinutes))
                .build();

        otpVerificationRepository.save(otp);
        emailService.sendOtpEmail(account.getEmail(), code);
        log.info("OTP sent to: {}", account.getEmail());
    }

    @Override
    @Transactional
    public boolean verify(UUID accountId, String code) {
        OtpVerification otp = otpVerificationRepository
                .findLatestActiveOtp(accountId)
                .orElse(null);

        if (otp == null || !otp.getCode().equals(code)) {
            log.warn("OTP verify failed for accountId: {}", accountId);
            return false;
        }

        otp.setUsed(true);
        otpVerificationRepository.save(otp);
        return true;
    }

    private String generateOtpCode() {
        int bound = (int) Math.pow(10, otpLength);
        String format = "%0" + otpLength + "d";
        return String.format(format, RANDOM.nextInt(bound));
    }
}
