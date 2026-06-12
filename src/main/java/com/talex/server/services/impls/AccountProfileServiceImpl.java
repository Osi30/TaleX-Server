package com.talex.server.services.impls;

import com.talex.server.configs.JwtTokenProvider;
import com.talex.server.dtos.requests.ChangePasswordRequest;
import com.talex.server.dtos.requests.ForgotPasswordRequest;
import com.talex.server.dtos.requests.ResetPasswordRequest;
import com.talex.server.dtos.requests.UpdateProfileRequest;
import com.talex.server.dtos.responses.AccountProfileResponse;
import com.talex.server.entities.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.services.AccountProfileService;
import com.talex.server.services.OtpService;
import com.talex.server.services.TokenFamilyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountProfileServiceImpl implements AccountProfileService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final TokenFamilyService tokenFamilyService;

    @Override
    public AccountProfileResponse getProfile(UUID accountId) {
        Account account = findActiveAccount(accountId);
        return toProfileResponse(account);
    }

    @Override
    @Transactional
    public AccountProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request) {
        Account account = findActiveAccount(accountId);

        if (request.getUsername() != null) {
            if (!account.getUsername().equals(request.getUsername())
                    && accountRepository.existsByUsernameAndAccountIdNot(
                            request.getUsername(), accountId)) {
                throw new AuthException(AuthErrorCode.USERNAME_ALREADY_EXISTS);
            }
            account.setUsername(request.getUsername());
        }
        if (request.getFullName() != null) {
            account.setFullName(request.getFullName());
        }
        if (request.getPhone() != null) {
            account.setPhone(request.getPhone());
        }
        if (request.getDateOfBirth() != null) {
            account.setDateOfBirth(request.getDateOfBirth());
        }
        if (request.getAvatarUrl() != null) {
            account.setAvatarUrl(request.getAvatarUrl());
        }

        accountRepository.save(account);
        log.info("Profile updated for accountId: {}", accountId);
        return toProfileResponse(account);
    }

    @Override
    @Transactional
    public void changePassword(UUID accountId, ChangePasswordRequest request) {
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());

        Account account = findActiveAccount(accountId);

        if (account.getPassword() != null) {
            // User has existing password — must verify current password
            if (request.getCurrentPassword() == null || request.getCurrentPassword().isBlank()) {
                throw new AuthException(AuthErrorCode.CURRENT_PASSWORD_REQUIRED);
            }
            if (!passwordEncoder.matches(request.getCurrentPassword(), account.getPassword())) {
                throw new AuthException(AuthErrorCode.CURRENT_PASSWORD_INCORRECT);
            }
            if (passwordEncoder.matches(request.getNewPassword(), account.getPassword())) {
                throw new AuthException(AuthErrorCode.PASSWORD_SAME_AS_OLD);
            }
        }
        // Google-only user (password == null) — allow setting password without currentPassword

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        tokenFamilyService.deleteAllFamilies(accountId);
        log.info("Password changed for accountId: {}, all sessions revoked", accountId);
    }

    @Override
    public String forgotPassword(ForgotPasswordRequest request) {
        // 1 email = 1 account — simple lookup
        Account account = accountRepository.findByEmail(request.getEmail()).orElse(null);

        if (account == null || account.getStatus() != AccountStatus.ACTIVE) {
            log.debug("Forgot password for non-existent or inactive email: {}", request.getEmail());
            // Anti-enumeration: return dummy token
            return jwtTokenProvider.generateVerificationToken(UUID.randomUUID());
        }

        otpService.enforcePasswordResetCooldown(account.getAccountId());
        otpService.generateAndSendPasswordReset(account);
        log.info("Forgot password OTP sent for accountId: {}", account.getAccountId());
        return jwtTokenProvider.generateVerificationToken(account.getAccountId());
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        validatePasswordConfirmation(request.getNewPassword(), request.getConfirmPassword());

        UUID accountId = jwtTokenProvider.extractVerificationAccountId(
                request.getVerificationToken());

        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }

        otpService.verifyPasswordReset(accountId, request.getOtpCode());

        account.setPassword(passwordEncoder.encode(request.getNewPassword()));
        accountRepository.save(account);

        tokenFamilyService.deleteAllFamilies(accountId);
        log.info("Password reset for accountId: {}, all sessions revoked", accountId);
    }

    private Account findActiveAccount(UUID accountId) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new AuthException(AuthErrorCode.INVALID_CREDENTIALS));

        if (account.getStatus() != AccountStatus.ACTIVE) {
            throw new AuthException(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
        }
        return account;
    }

    private AccountProfileResponse toProfileResponse(Account account) {
        return AccountProfileResponse.builder()
                .accountId(account.getAccountId().toString())
                .username(account.getUsername())
                .email(account.getEmail())
                .fullName(account.getFullName())
                .phone(account.getPhone())
                .dateOfBirth(account.getDateOfBirth())
                .avatarUrl(account.getAvatarUrl())
                .hasPassword(account.getPassword() != null)
                .googleLinked(account.getGoogleSubId() != null)
                .roleName(account.getRole().getCode())
                .status(account.getStatus().name())
                .createdAt(account.getCreatedAt())
                .build();
    }

    private void validatePasswordConfirmation(String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new AuthException(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
        }
    }
}
