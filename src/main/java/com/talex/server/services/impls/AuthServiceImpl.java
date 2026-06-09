package com.talex.server.services.impls;

import com.talex.server.configs.JwtTokenProvider;
import com.talex.server.dtos.requests.*;
import com.talex.server.dtos.responses.AccountProfileResponse;
import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.dtos.responses.GoogleUserInfo;
import com.talex.server.entities.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.services.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        var existing = accountRepository.findByEmail(request.getEmail()).orElse(null);

        if (existing != null) {
            if (existing.getStatus() == AccountStatus.VERIFYING) {
                // Re-register while still verifying — update info and resend OTP
                updateVerifyingAccount(existing, request);
                accountRepository.save(existing);
                otpService.generateAndSend(existing);
                log.info("Re-register (VERIFYING) for: {}", request.getEmail());
                return jwtTokenProvider.generateVerificationToken(existing.getAccountId());
            }
            throw new AuthException(AuthErrorCode.EMAIL_ALREADY_EXISTS);
        }

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

    @Override
    public AuthResponse login(LoginRequest request) {
        Account account = accountRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        validateAccountStatus(account);

        if (account.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            throw new AuthException(AuthErrorCode.INVALID_CREDENTIALS);
        }

        log.info("Login success: {}", request.getEmail());
        return generateAuthResponse(account);
    }

    @Override
    @Transactional
    public Object googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleInfo = googleAuthService.verifyIdToken(request.getIdToken());

        // Find by googleSubId first, then by email
        Account account = accountRepository.findByGoogleSubId(googleInfo.getGoogleSubId())
                .orElse(null);

        if (account == null) {
            account = accountRepository.findByEmail(googleInfo.getEmail()).orElse(null);
        }

        if (account != null) {
            // Link googleSubId if not yet linked
            if (account.getGoogleSubId() == null) {
                account.setGoogleSubId(googleInfo.getGoogleSubId());
                accountRepository.save(account);
            }

            return switch (account.getStatus()) {
                case ACTIVE -> generateAuthResponse(account);
                case ONBOARDING -> jwtTokenProvider.generateVerificationToken(account.getAccountId());
                case VERIFYING -> {
                    otpService.generateAndSend(account);
                    yield jwtTokenProvider.generateVerificationToken(account.getAccountId());
                }
                case BANNED -> throw new AuthException(AuthErrorCode.ACCOUNT_BANNED);
                case DELETED -> throw new AuthException(AuthErrorCode.ACCOUNT_DELETED);
            };
        }

        // New Google account — create as ONBOARDING (no OTP needed, email verified by Google)
        String username = generateUsernameFromEmail(googleInfo.getEmail());

        Account newAccount = Account.builder()
                .username(username)
                .email(googleInfo.getEmail())
                .googleSubId(googleInfo.getGoogleSubId())
                .fullName(googleInfo.getName())
                .status(AccountStatus.ONBOARDING)
                .role(roleService.findByCode("VIEWER"))
                .build();

        accountRepository.save(newAccount);

        log.info("New Google account (ONBOARDING): {}", googleInfo.getEmail());
        return jwtTokenProvider.generateVerificationToken(newAccount.getAccountId());
    }

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
    public void forgotPassword(ForgotPasswordRequest request) {
        accountProfileService.forgotPassword(request);
    }

    @Override
    public void resetPassword(ResetPasswordRequest request) {
        accountProfileService.resetPassword(request);
    }

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

    private void updateVerifyingAccount(Account account, RegisterRequest request) {
        account.setPassword(passwordEncoder.encode(request.getPassword()));
        account.setFullName(request.getFullName());
        account.setDateOfBirth(request.getDateOfBirth());
        account.setPhone(request.getPhone());

        // Update username only if changed and not taken by another account
        if (!account.getUsername().equals(request.getUsername())) {
            if (accountRepository.existsByUsername(request.getUsername())) {
                throw new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
            }
            account.setUsername(request.getUsername());
        }
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0];
        if (!accountRepository.existsByUsername(base)) {
            return base;
        }
        return base + "_" + System.currentTimeMillis() % 10000;
    }
}
