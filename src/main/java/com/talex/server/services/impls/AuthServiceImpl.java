package com.talex.server.services.impls;

import com.talex.server.configs.JwtTokenProvider;
import com.talex.server.dtos.requests.*;
import com.talex.server.dtos.responses.AccountProfileResponse;
import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.dtos.responses.GoogleAuthResponseDto;
import com.talex.server.dtos.responses.GoogleUserInfo;
import com.talex.server.entities.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final TokenFamilyService tokenFamilyService;
    private final GoogleAuthService googleAuthService;
    private final AccountProfileService accountProfileService;
    private final StringRedisTemplate redisTemplate;

    private static final String LOGIN_FAIL_PREFIX = "login_fail:";
    private static final int MAX_LOGIN_ATTEMPTS = 5;

    @Value("${login.rate-limit-minutes:15}")
    private int rateLimitMinutes;

    // ── Register ────────────────────────────────────────────────────

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        // 1 email = 1 account — check email unique
        var existingByEmail = accountRepository.findByEmail(request.getEmail()).orElse(null);

        if (existingByEmail != null) {
            if (existingByEmail.getStatus() == AccountStatus.VERIFYING) {
                // Re-register while still verifying — update info and resend OTP
                updateVerifyingAccount(existingByEmail, request);
                accountRepository.save(existingByEmail);
                otpService.generateAndSend(existingByEmail);
                log.info("Re-register (VERIFYING) for: {}", request.getEmail());
                return jwtTokenProvider.generateVerificationToken(existingByEmail.getAccountId());
            }
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

        // Check username unique
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
        }

        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .phone(request.getPhone())
                .role(roleService.findByCode("VIEWER"))
                .build();

        accountRepository.save(account);
        otpService.generateAndSend(account);

        log.info("User registered: {}", request.getEmail());
        return jwtTokenProvider.generateVerificationToken(account.getAccountId());
    }

    // ── Verify Email ────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyOtpRequest request) {
        UUID accountId = jwtTokenProvider.extractVerificationAccountId(
                request.getVerificationToken());

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN));

        if (account.getStatus() != AccountStatus.VERIFYING) {
            throw new AuthException(AuthErrorCode.ACCOUNT_NOT_VERIFIED,
                    "Account is not in verifying state");
        }

        otpService.verify(accountId, request.getOtpCode());

        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        log.info("Email verified for accountId: {}", accountId);
        return generateAuthResponse(account);
    }

    // ── Login (email + password) with rate limiting ─────────────────

    @Override
    public AuthResponse login(LoginRequest request) {
        // Rate limit check
        enforceLoginRateLimit(request.getEmail());

        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> {
                    incrementLoginFail(request.getEmail());
                    return new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
                });

        if (account.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            incrementLoginFail(request.getEmail());
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        validateAccountStatus(account);

        // Login success — clear fail counter
        clearLoginFail(request.getEmail());

        log.info("Login success: {}", request.getEmail());
        return generateAuthResponse(account);
    }

    // ── Google Login ────────────────────────────────────────────────

    @Override
    @Transactional
    public GoogleAuthResponseDto googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleInfo = googleAuthService.verifyIdToken(request.getIdToken());

        // Step 1: Find by googleSubId (returning user)
        Account account = accountRepository.findByGoogleSubId(googleInfo.getGoogleSubId())
                .orElse(null);

        if (account != null) {
            return handleExistingGoogleAccount(account);
        }

        // Step 2: Find by email — link Google to existing account
        account = accountRepository.findByEmail(googleInfo.getEmail()).orElse(null);

        if (account != null) {
            return linkGoogleToExistingAccount(account, googleInfo);
        }

        // Step 3: New user — create account
        return createNewGoogleAccount(googleInfo);
    }

    // ── Complete Profile (Google onboarding) ─────────────────────────

    @Override
    @Transactional
    public AuthResponse completeProfile(CompleteProfileRequest request) {
        UUID accountId = jwtTokenProvider.extractVerificationAccountId(
                request.getVerificationToken());

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN));

        if (account.getStatus() != AccountStatus.ONBOARDING) {
            throw new AuthException(AuthErrorCode.PROFILE_INCOMPLETE,
                    "Account is not in onboarding state");
        }

        account.setPhone(request.getPhone());
        account.setDateOfBirth(request.getDateOfBirth());
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        log.info("Profile completed for accountId: {}", accountId);
        return generateAuthResponse(account);
    }

    // ── Token Management ────────────────────────────────────────────

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String newRefreshToken = tokenFamilyService.validateAndRotate(
                request.getRefreshToken());

        UUID accountId = tokenFamilyService.extractAccountId(newRefreshToken);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.SESSION_EXPIRED));

        validateAccountStatus(account);

        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(account))
                .refreshToken(newRefreshToken)
                .build();
    }

    @Override
    public void logout(RefreshTokenRequest request) {
        UUID accountId = tokenFamilyService.extractAccountId(
                request.getRefreshToken());
        tokenFamilyService.deleteFamily(request.getRefreshToken());
        log.info("User logged out: {}", accountId);
    }

    // ── OTP ─────────────────────────────────────────────────────────

    @Override
    @Transactional
    public String resendOtp(ResendOtpRequest request) {
        UUID accountId = jwtTokenProvider.extractVerificationAccountId(
                request.getVerificationToken());

        otpService.enforceResendCooldown(accountId);

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN));

        if (account.getStatus() != AccountStatus.VERIFYING) {
            throw new AuthException(AuthErrorCode.ACCOUNT_NOT_VERIFIED,
                    "Account is not in verifying state");
        }

        otpService.generateAndSend(account);
        return "OTP mới đã được gửi tới email của bạn";
    }

    // ── Profile Delegation ──────────────────────────────────────────

    @Override
    public AccountProfileResponse getProfile(UUID accountId) {
        return accountProfileService.getProfile(accountId);
    }

    @Override
    public AccountProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
        return accountProfileService.updateProfile(accountId, request);
    }

    @Override
    public void changePassword(UUID accountId, ChangePasswordRequest request) {
        accountProfileService.changePassword(accountId, request);
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        return accountProfileService.forgotPassword(request);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        accountProfileService.resetPassword(request);
    }

    // ── Private: Google Login Helpers ────────────────────────────────

    private GoogleAuthResponseDto handleExistingGoogleAccount(Account account) {
        return switch (account.getStatus()) {
            case ACTIVE -> toGoogleAuthResponse("ACTIVE", account);
            case ONBOARDING -> GoogleAuthResponseDto.builder()
                    .status("ONBOARDING")
                    .verificationToken(jwtTokenProvider.generateVerificationToken(account.getAccountId()))
                    .build();
            case VERIFYING -> {
                otpService.generateAndSend(account);
                yield GoogleAuthResponseDto.builder()
                        .status("VERIFYING")
                        .verificationToken(jwtTokenProvider.generateVerificationToken(account.getAccountId()))
                        .build();
            }
            case BANNED -> throw new AuthException(AuthErrorCode.ACCOUNT_BANNED);
            case DELETED -> throw new AuthException(AuthErrorCode.ACCOUNT_DELETED);
        };
    }

    private GoogleAuthResponseDto linkGoogleToExistingAccount(Account account, GoogleUserInfo googleInfo) {
        // Link Google identity to existing account
        account.setGoogleSubId(googleInfo.getGoogleSubId());
        if (account.getAvatarUrl() == null && googleInfo.getPictureUrl() != null) {
            account.setAvatarUrl(googleInfo.getPictureUrl());
        }
        accountRepository.save(account);

        log.info("Linked Google to existing account: {}", account.getEmail());
        return handleExistingGoogleAccount(account);
    }

    private GoogleAuthResponseDto createNewGoogleAccount(GoogleUserInfo googleInfo) {
        String username = generateUsernameFromEmail(googleInfo.getEmail());

        Account newAccount = Account.builder()
                .username(username)
                .email(googleInfo.getEmail())
                .googleSubId(googleInfo.getGoogleSubId())
                .fullName(googleInfo.getName())
                .avatarUrl(googleInfo.getPictureUrl())
                .status(AccountStatus.ONBOARDING)
                .role(roleService.findByCode("VIEWER"))
                .build();

        accountRepository.save(newAccount);

        log.info("New Google account (ONBOARDING): {}", googleInfo.getEmail());
        return GoogleAuthResponseDto.builder()
                .status("ONBOARDING")
                .verificationToken(jwtTokenProvider.generateVerificationToken(newAccount.getAccountId()))
                .build();
    }

    // ── Private: Rate Limiting ──────────────────────────────────────

    private void enforceLoginRateLimit(String email) {
        String key = LOGIN_FAIL_PREFIX + email;
        String value = redisTemplate.opsForValue().get(key);
        if (value != null && Integer.parseInt(value) >= MAX_LOGIN_ATTEMPTS) {
            throw new AuthException(AuthErrorCode.LOGIN_RATE_LIMITED);
        }
    }

    private void incrementLoginFail(String email) {
        String key = LOGIN_FAIL_PREFIX + email;
        Long count = redisTemplate.opsForValue().increment(key);
        if (count != null && count == 1) {
            redisTemplate.expire(key, Duration.ofMinutes(rateLimitMinutes));
        }
    }

    private void clearLoginFail(String email) {
        redisTemplate.delete(LOGIN_FAIL_PREFIX + email);
    }

    // ── Private: Common Helpers ─────────────────────────────────────

    private void validateAccountStatus(Account account) {
        switch (account.getStatus()) {
            case ACTIVE -> { /* OK */ }
            case VERIFYING -> throw new AuthException(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
            case ONBOARDING -> throw new AuthException(AuthErrorCode.PROFILE_INCOMPLETE);
            case BANNED -> throw new AuthException(AuthErrorCode.ACCOUNT_BANNED);
            case DELETED -> throw new AuthException(AuthErrorCode.ACCOUNT_DELETED);
        }
    }

    private AuthResponse generateAuthResponse(Account account) {
        return AuthResponse.builder()
                .accessToken(jwtTokenProvider.generateAccessToken(account))
                .refreshToken(tokenFamilyService.createFamily(account.getAccountId()))
                .build();
    }

    private GoogleAuthResponseDto toGoogleAuthResponse(String status, Account account) {
        AuthResponse auth = generateAuthResponse(account);
        return GoogleAuthResponseDto.builder()
                .status(status)
                .accessToken(auth.getAccessToken())
                .refreshToken(auth.getRefreshToken())
                .build();
    }

    private void updateVerifyingAccount(Account account, RegisterRequest request) {
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());
        account.setDateOfBirth(request.getDateOfBirth());
        account.setPhone(request.getPhone());

        if (!account.getUsername().equals(request.getUsername())) {
            if (accountRepository.existsByUsername(request.getUsername())) {
                throw new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
            }
            account.setUsername(request.getUsername());
        }
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0];
        return base + "_" + UUID.randomUUID().toString().substring(0, 6);
    }
}
