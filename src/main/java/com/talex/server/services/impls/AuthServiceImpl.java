package com.talex.server.services.impls;

import com.talex.server.configs.JwtTokenProvider;
import com.talex.server.dtos.requests.CompleteRegistrationRequest;
import com.talex.server.dtos.requests.GoogleLoginRequest;
import com.talex.server.dtos.requests.LoginRequest;
import com.talex.server.dtos.requests.RefreshTokenRequest;
import com.talex.server.dtos.requests.RegisterRequest;
import com.talex.server.dtos.requests.ResendOtpRequest;
import com.talex.server.dtos.requests.VerifyOtpRequest;
import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.dtos.responses.GoogleUserInfo;
import com.talex.server.entities.Account;
import com.talex.server.entities.Role;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.BadRequestException;
import com.talex.server.exceptions.ConflictException;
import com.talex.server.exceptions.ResourceNotFoundException;
import com.talex.server.exceptions.UnauthorizedException;
import com.talex.server.mappers.AccountMapper;
import com.talex.server.repositories.AccountRepository;
import java.util.UUID;
import com.talex.server.repositories.RoleRepository;
import com.talex.server.services.AuthService;
import com.talex.server.services.GoogleAuthService;
import com.talex.server.services.OtpService;
import com.talex.server.services.TokenFamilyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthServiceImpl implements AuthService {

    private final AccountRepository accountRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenProvider jwtTokenProvider;
    private final OtpService otpService;
    private final TokenFamilyService tokenFamilyService;
    private final GoogleAuthService googleAuthService;
    private final AccountMapper accountMapper;

    @Value("${jwt.access-token-expiration}")
    private long accessTokenExpirationMs;

    @Override
    @Transactional
    public String register(RegisterRequest request) {
        if (accountRepository.existsByEmail(request.getEmail())) {
            throw new ConflictException("Email already exists");
        }
        if (accountRepository.existsByUsername(request.getUsername())) {
            throw new ConflictException("Username already exists");
        }

        Role viewerRole = roleRepository.findByCode("VIEWER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role VIEWER not found"));

        Account account = Account.builder()
                .username(request.getUsername())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .phone(request.getPhone())
                .role(viewerRole)
                .build();

        accountRepository.save(account);
        otpService.generateAndSend(account);

        log.info("User registered: {}", request.getEmail());
        return "OTP đã gửi tới email " + request.getEmail();
    }

    @Override
    @Transactional
    public AuthResponse verifyEmail(VerifyOtpRequest request) {
        Account account = findAccountByEmail(request.getEmail());

        if (account.getStatus() != AccountStatus.VERIFYING) {
            throw new BadRequestException("Account already verified");
        }

        if (!otpService.verify(account.getAccountId(), request.getOtpCode())) {
            throw new BadRequestException("Invalid or expired OTP");
        }

        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        log.info("Email verified: {}", request.getEmail());
        return generateAuthResponse(account);
    }

    @Override
    public AuthResponse login(LoginRequest request) {
        Account account = findAccountByEmail(request.getEmail());

        validateAccountStatus(account);

        if (account.getPassword() == null
                || !passwordEncoder.matches(request.getPassword(), account.getPassword())) {
            log.warn("Login failed: {}", request.getEmail());
            throw new UnauthorizedException("Invalid credentials");
        }

        log.info("Login success: {}", request.getEmail());
        return generateAuthResponse(account);
    }

    @Override
    @Transactional
    public AuthResponse googleLogin(GoogleLoginRequest request) {
        GoogleUserInfo googleInfo = googleAuthService.verifyIdToken(request.getIdToken());

        Account account = accountRepository.findByGoogleSubId(googleInfo.getGoogleSubId())
                .orElseGet(() -> findOrCreateGoogleAccount(googleInfo));

        validateAccountStatus(account);

        log.info("Google login: {}", account.getEmail());
        return generateAuthResponse(account);
    }

    @Override
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        String newRefreshToken = tokenFamilyService.validateAndRotate(
                request.getRefreshToken());

        UUID accountId = tokenFamilyService.extractAccountId(newRefreshToken);
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        // Block refresh if account is no longer active
        validateAccountStatus(account);

        String accessToken = jwtTokenProvider.generateAccessToken(account);

        return accountMapper.toAuthResponse(
                account, accessToken, newRefreshToken,
                accessTokenExpirationMs / 1000);
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
        Account account = findAccountByEmail(request.getEmail());

        if (account.getStatus() != AccountStatus.VERIFYING) {
            throw new BadRequestException("Account already verified");
        }

        otpService.generateAndSend(account);
        return "OTP mới đã gửi tới email " + request.getEmail();
    }

    @Override
    @Transactional
    public AuthResponse completeRegistration(UUID accountId,
                                             CompleteRegistrationRequest request) {
        Account account = accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));

        if (account.getStatus() != AccountStatus.INCOMPLETE) {
            throw new BadRequestException("Account registration is already complete");
        }

        account.setDateOfBirth(request.getDateOfBirth());
        account.setPhone(request.getPhone());
        account.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(account);

        log.info("Registration completed for account: {}", accountId);
        return generateAuthResponse(account);
    }

    private void validateAccountStatus(Account account) {
        switch (account.getStatus()) {
            case ACTIVE, INCOMPLETE -> { /* OK — AccountStatusFilter restricts INCOMPLETE */ }
            case VERIFYING -> throw new UnauthorizedException("Please verify your email first");
            case BANNED -> throw new UnauthorizedException("Account has been banned");
            case DELETED -> throw new UnauthorizedException("Account has been deleted");
        }
    }

    private Account findAccountByEmail(String email) {
        return accountRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found"));
    }

    private Account findOrCreateGoogleAccount(GoogleUserInfo googleInfo) {
        // Check if email already exists (user registered with email/password before)
        return accountRepository.findByEmail(googleInfo.getEmail())
                .map(existing -> {
                    existing.setGoogleSubId(googleInfo.getGoogleSubId());
                    if (existing.getStatus() == AccountStatus.VERIFYING) {
                        existing.setStatus(AccountStatus.ACTIVE);
                    }
                    return accountRepository.save(existing);
                })
                .orElseGet(() -> createNewGoogleAccount(googleInfo));
    }

    private Account createNewGoogleAccount(GoogleUserInfo googleInfo) {
        Role viewerRole = roleRepository.findByCode("VIEWER")
                .orElseThrow(() -> new ResourceNotFoundException("Default role VIEWER not found"));

        String username = generateUsernameFromEmail(googleInfo.getEmail());

        Account account = Account.builder()
                .username(username)
                .email(googleInfo.getEmail())
                .googleSubId(googleInfo.getGoogleSubId())
                .fullName(googleInfo.getName())
                .status(AccountStatus.INCOMPLETE)
                .role(viewerRole)
                .build();

        return accountRepository.save(account);
    }

    private String generateUsernameFromEmail(String email) {
        String base = email.split("@")[0];
        if (!accountRepository.existsByUsername(base)) {
            return base;
        }
        // Append random suffix if username taken
        return base + "_" + System.currentTimeMillis() % 10000;
    }

    private AuthResponse generateAuthResponse(Account account) {
        String accessToken = jwtTokenProvider.generateAccessToken(account);
        String refreshToken = tokenFamilyService.createFamily(account.getAccountId());
        return accountMapper.toAuthResponse(
                account, accessToken, refreshToken,
                accessTokenExpirationMs / 1000);
    }
}
