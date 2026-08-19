package com.talex.server.services.auth;

import com.talex.server.configs.JwtTokenProvider;
import com.talex.server.dtos.requests.auth.*;
import com.talex.server.dtos.responses.auth.AuthResponse;
import com.talex.server.dtos.responses.auth.GoogleAuthResponseDto;
import com.talex.server.dtos.responses.auth.GoogleUserInfo;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.auth.Role;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.auth.impls.AuthServiceImpl;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthServiceImpl Tests")
class AuthServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private RoleService roleService;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private TokenFamilyService tokenFamilyService;

    @Mock
    private GoogleAuthService googleAuthService;

    @Mock
    private AccountProfileService accountProfileService;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private com.talex.server.services.auth.impls.GoogleAccountCreationExecutor googleAccountCreationExecutor;

    @InjectMocks
    private AuthServiceImpl authService;

    // AuthServiceImpl.register() KHÔNG tự validate field null/format/length — các rule đó
    // là Bean Validation annotation (@NotBlank/@Email/@Size) trên RegisterRequest, chỉ được
    // Spring kích hoạt qua @Valid ở tầng Controller. Gọi thẳng service (bỏ qua Controller)
    // sẽ KHÔNG throw cho input null/rỗng — validate trực tiếp DTO bằng Validator là cách
    // đúng để unit test những rule này mà không cần dựng context Spring/MockMvc.
    private static Validator validator;

    private RegisterRequest registerRequest;
    private Account testAccount;
    private Role viewerRole;
    private UUID testAccountId;
    private String testEmail;
    private String testPassword;
    private String testUsername;

    @BeforeEach
    void setUp() {
        if (validator == null) {
            validator = Validation.buildDefaultValidatorFactory().getValidator();
        }
        testAccountId = UUID.randomUUID();
        testEmail = "user@example.com";
        testPassword = "Password123!";
        testUsername = "testuser";

        viewerRole = Role.builder()
                .roleId(1L)
                .code("VIEWER")
                .roleName("Viewer")
                .build();

        registerRequest = new RegisterRequest();
        registerRequest.setEmail(testEmail);
        registerRequest.setUsername(testUsername);
        registerRequest.setPassword(testPassword);
        registerRequest.setFullName("Test User");
        registerRequest.setDateOfBirth(LocalDate.of(2000, 1, 1));
        registerRequest.setPhone("0912345678");

        // KHÔNG gọi passwordEncoder.encode(testPassword) ở đây — passwordEncoder là mock
        // chưa stub tại thời điểm này, gọi method chưa stub trả về null (Object return type),
        // khiến testAccount.getPassword() thành null. login() có check "account.getPassword()
        // == null" TRƯỚC KHI gọi matches(), nên mọi test login sẽ luôn rơi vào nhánh
        // INVALID_CREDENTIALS bất kể matches() được stub true hay không. Dùng literal cố định.
        testAccount = Account.builder()
                .accountId(testAccountId)
                .email(testEmail)
                .username(testUsername)
                .password("encodedPassword123")
                .status(AccountStatus.VERIFYING)
                .role(viewerRole)
                .lastInteractionTime(LocalDateTime.now())
                .build();

        // Setup rate limiting
        ReflectionTestUtils.setField(authService, "rateLimitMinutes", 15);
    }

    // ==================== REGISTER TESTS ====================

    @Test
    @DisplayName("register: should create new account when email is new")
    void testRegisterSuccessfulNewAccount() {
        // Arrange: UTCID01 - Normal case: new account with valid credentials
        when(accountRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername(testUsername)).thenReturn(false);
        when(roleService.findByCode("VIEWER")).thenReturn(viewerRole);
        when(accountRepository.save(any(Account.class))).thenReturn(testAccount);
        // register() KHÔNG gán lại kết quả accountRepository.save(account) vào biến local
        // (chỉ gọi save(account) rồi bỏ qua return value) — accountId của entity build tại
        // chỗ luôn là null lúc test (chỉ thật sự được Hibernate gán khi có session DB thật,
        // @GeneratedValue chạy lúc flush, không xảy ra với repository đã mock). Vì vậy
        // generateVerificationToken() được gọi với null, không phải testAccountId.
        when(jwtTokenProvider.generateVerificationToken(isNull())).thenReturn("verification_token");

        // Act
        String result = authService.register(registerRequest);

        // Assert
        assertThat(result).isEqualTo("verification_token");
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(otpService, times(1)).generateAndSend(any(Account.class));
        verify(jwtTokenProvider, times(1)).generateVerificationToken(isNull());
    }

    @Test
    @DisplayName("register: should allow re-register when account is VERIFYING")
    void testRegisterReRegisterWhileVerifying() {
        // Arrange: UTCID02 - Re-register while account is VERIFYING
        Account existingAccount = Account.builder()
                .accountId(testAccountId)
                .email(testEmail)
                .username("oldusername")
                .status(AccountStatus.VERIFYING)
                .role(viewerRole)
                .build();

        when(accountRepository.findByEmail(testEmail)).thenReturn(Optional.of(existingAccount));
        when(accountRepository.existsByUsername(testUsername)).thenReturn(false);
        when(accountRepository.save(any(Account.class))).thenReturn(existingAccount);
        when(jwtTokenProvider.generateVerificationToken(testAccountId)).thenReturn("new_verification_token");

        // Act
        String result = authService.register(registerRequest);

        // Assert
        assertThat(result).isEqualTo("new_verification_token");
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(otpService, times(1)).generateAndSend(any(Account.class));
    }

    @Test
    @DisplayName("register: should throw EMAIL_ALREADY_EXISTS when email belongs to ACTIVE account")
    void testRegisterEmailAlreadyExists() {
        // Arrange: UTCID03 - Email already exists in ACTIVE account
        Account activeAccount = Account.builder()
                .accountId(UUID.randomUUID())
                .email(testEmail)
                .status(AccountStatus.ACTIVE)
                .build();

        when(accountRepository.findByEmail(testEmail)).thenReturn(Optional.of(activeAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.EMAIL_ALREADY_EXISTS);

        verify(accountRepository, never()).save(any(Account.class));
    }

    // 6 test dưới đây validate trực tiếp Bean Validation constraint trên RegisterRequest
    // (KHÔNG gọi authService.register()) — xem giải thích ở field `validator` phía trên.

    @Test
    @DisplayName("register: RegisterRequest validation rejects null email")
    void testRegisterEmailNull() {
        // UTCID04 - Email is null
        registerRequest.setEmail(null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("register: RegisterRequest validation rejects invalid email format")
    void testRegisterInvalidEmailFormat() {
        // UTCID05 - Invalid email format
        registerRequest.setEmail("invalid-email");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("email"));
    }

    @Test
    @DisplayName("register: RegisterRequest validation rejects null username")
    void testRegisterUsernameNull() {
        // UTCID06 - Username is null
        registerRequest.setUsername(null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("register: RegisterRequest validation rejects username shorter than min length")
    void testRegisterUsernameTooShort() {
        // UTCID07 - Boundary case: username with 2 characters (min is 3)
        registerRequest.setUsername("ab");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("username"));
    }

    @Test
    @DisplayName("register: RegisterRequest validation rejects null password")
    void testRegisterPasswordNull() {
        // UTCID08 - Password is null
        registerRequest.setPassword(null);

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("register: RegisterRequest validation rejects password shorter than min length")
    void testRegisterPasswordTooShort() {
        // UTCID09 - Boundary case: password with 7 characters (min is 8)
        registerRequest.setPassword("Pass1!7");

        Set<ConstraintViolation<RegisterRequest>> violations = validator.validate(registerRequest);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("password"));
    }

    @Test
    @DisplayName("register: should throw USERNAME_ALREADY_EXISTS when username is taken")
    void testRegisterUsernameAlreadyExists() {
        // Arrange: UTCID10 - Username already exists (even though email is new)
        when(accountRepository.findByEmail(testEmail)).thenReturn(Optional.empty());
        when(accountRepository.existsByUsername(testUsername)).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.register(registerRequest))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.USERNAME_ALREADY_EXISTS);

        verify(accountRepository, never()).save(any(Account.class));
    }

    // ==================== VERIFY EMAIL TESTS ====================

    @Test
    @DisplayName("verifyEmail: should activate account when OTP is valid")
    void testVerifyEmailSuccess() {
        // Arrange: UTCID01 - Normal case: valid verification token and OTP
        String verificationToken = "valid_token";
        String otpCode = "123456";
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(verificationToken);
        request.setOtpCode(otpCode);

        when(jwtTokenProvider.extractVerificationAccountId(verificationToken))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(any(Account.class)))
                .thenReturn("access_token");
        when(tokenFamilyService.createFamily(testAccountId))
                .thenReturn("refresh_token");

        // Act
        AuthResponse response = authService.verifyEmail(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(otpService, times(1)).verify(testAccountId, otpCode);
    }

    @Test
    @DisplayName("verifyEmail: should throw INVALID_VERIFICATION_TOKEN when token is invalid")
    void testVerifyEmailInvalidToken() {
        // Arrange: UTCID02 - Invalid verification token
        String invalidToken = "invalid_token";
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(invalidToken);
        request.setOtpCode("123456");

        when(jwtTokenProvider.extractVerificationAccountId(invalidToken))
                .thenThrow(new AuthException(AuthErrorCode.INVALID_VERIFICATION_TOKEN));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    @Test
    @DisplayName("verifyEmail: should throw ACCOUNT_NOT_VERIFIED when account status is not VERIFYING")
    void testVerifyEmailAccountNotVerifying() {
        // Arrange: UTCID03 - Account is already ACTIVE, not VERIFYING
        String token = "valid_token";
        Account activeAccount = Account.builder()
                .accountId(testAccountId)
                .status(AccountStatus.ACTIVE)
                .build();

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(token);
        request.setOtpCode("123456");

        when(jwtTokenProvider.extractVerificationAccountId(token))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(activeAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("verifyEmail: should throw error when OTP verification fails")
    void testVerifyEmailOtpInvalid() {
        // Arrange: UTCID04 - OTP code is invalid
        String token = "valid_token";
        String invalidOtp = "000000";
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(token);
        request.setOtpCode(invalidOtp);

        when(jwtTokenProvider.extractVerificationAccountId(token))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        doThrow(new AuthException(AuthErrorCode.INVALID_OTP))
                .when(otpService).verify(testAccountId, invalidOtp);

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_OTP);
    }

    @Test
    @DisplayName("verifyEmail: should throw INVALID_VERIFICATION_TOKEN when account not found")
    void testVerifyEmailAccountNotFound() {
        // Arrange: UTCID05 - Account ID from token doesn't exist
        String token = "orphan_token";
        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(token);
        request.setOtpCode("123456");

        when(jwtTokenProvider.extractVerificationAccountId(token))
                .thenReturn(UUID.randomUUID());
        when(accountRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    @Test
    @DisplayName("verifyEmail: should handle BANNED account status")
    void testVerifyEmailBannedAccount() {
        // Arrange: UTCID06 - Account is BANNED
        String token = "valid_token";
        Account bannedAccount = Account.builder()
                .accountId(testAccountId)
                .status(AccountStatus.BANNED)
                .build();

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(token);
        request.setOtpCode("123456");

        when(jwtTokenProvider.extractVerificationAccountId(token))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(bannedAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("verifyEmail: should handle ONBOARDING account status")
    void testVerifyEmailOnboardingAccount() {
        // Arrange: UTCID07 - Account is in ONBOARDING status
        String token = "valid_token";
        Account onboardingAccount = Account.builder()
                .accountId(testAccountId)
                .status(AccountStatus.ONBOARDING)
                .build();

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(token);
        request.setOtpCode("123456");

        when(jwtTokenProvider.extractVerificationAccountId(token))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(onboardingAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("verifyEmail: should handle DELETED account status")
    void testVerifyEmailDeletedAccount() {
        // Arrange: UTCID08 - Account is DELETED
        String token = "valid_token";
        Account deletedAccount = Account.builder()
                .accountId(testAccountId)
                .status(AccountStatus.DELETED)
                .build();

        VerifyOtpRequest request = new VerifyOtpRequest();
        request.setVerificationToken(token);
        request.setOtpCode("123456");

        when(jwtTokenProvider.extractVerificationAccountId(token))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(deletedAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.verifyEmail(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    // ==================== LOGIN TESTS ====================

    @Test
    @DisplayName("login: should return tokens when credentials are valid")
    void testLoginSuccessful() {
        // Arrange: UTCID01 - Normal case: valid email and password
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.ACTIVE);
        // KHÔNG gọi passwordEncoder.encode() (mock chưa stub trả null) — dùng lại literal
        // "encodedPassword123" đã set sẵn ở setUp(), xem giải thích ở đó.

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, testAccount.getPassword()))
                .thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("access_token");
        when(tokenFamilyService.createFamily(testAccountId))
                .thenReturn("refresh_token");

        // Act
        AuthResponse response = authService.login(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
        verify(redisTemplate, times(1)).delete("login_fail:" + testEmail);
    }

    @Test
    @DisplayName("login: should throw INVALID_CREDENTIALS when email not found")
    void testLoginEmailNotFound() {
        // Arrange: UTCID02 - Email doesn't exist
        LoginRequest request = new LoginRequest();
        request.setEmail("nonexistent@example.com");
        request.setPassword(testPassword);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail("nonexistent@example.com"))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);

        // opsForValue() gọi 2 lần thật: enforceLoginRateLimit() (check) + incrementLoginFail()
        // (tăng bộ đếm khi email không tồn tại) — cả 2 đều thao tác Redis riêng biệt.
        verify(redisTemplate, times(2)).opsForValue();
    }

    @Test
    @DisplayName("login: should throw INVALID_CREDENTIALS when password is wrong")
    void testLoginInvalidPassword() {
        // Arrange: UTCID03 - Password doesn't match
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrongpassword");

        testAccount.setStatus(AccountStatus.ACTIVE);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongpassword", testAccount.getPassword()))
                .thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("login: should throw ACCOUNT_NOT_VERIFIED when account is VERIFYING")
    void testLoginAccountNotVerified() {
        // Arrange: UTCID04 - Account status is VERIFYING
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.VERIFYING);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, testAccount.getPassword()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("login: should throw PROFILE_INCOMPLETE when account is ONBOARDING")
    void testLoginProfileIncomplete() {
        // Arrange: UTCID05 - Account status is ONBOARDING
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.ONBOARDING);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, testAccount.getPassword()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.PROFILE_INCOMPLETE);
    }

    @Test
    @DisplayName("login: should throw ACCOUNT_BANNED when account is BANNED")
    void testLoginAccountBanned() {
        // Arrange: UTCID06 - Account status is BANNED
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.BANNED);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, testAccount.getPassword()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_BANNED);
    }

    @Test
    @DisplayName("login: should throw ACCOUNT_DELETED when account is DELETED")
    void testLoginAccountDeleted() {
        // Arrange: UTCID07 - Account status is DELETED
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.DELETED);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, testAccount.getPassword()))
                .thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_DELETED);
    }

    @Test
    @DisplayName("login: should throw LOGIN_RATE_LIMITED after max failed attempts")
    void testLoginRateLimited() {
        // Arrange: UTCID08 - Rate limiting: 5 failed attempts
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get("login_fail:" + testEmail)).thenReturn("5"); // 5 failures

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.LOGIN_RATE_LIMITED);

        verify(accountRepository, never()).findByEmail(anyString());
    }

    @Test
    @DisplayName("login: should increment fail counter on failed attempt")
    void testLoginFailCounterIncrement() {
        // Arrange: UTCID09 - Track failed attempt increments counter
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword("wrong");

        testAccount.setStatus(AccountStatus.ACTIVE);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrong", testAccount.getPassword()))
                .thenReturn(false);

        // Act
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class);

        // Assert: verify increment and expire called
        verify(mockOps).increment("login_fail:" + testEmail);
        verify(redisTemplate).expire(eq("login_fail:" + testEmail), any(Duration.class));
    }

    @Test
    @DisplayName("login: should handle account with null password")
    void testLoginNullPassword() {
        // Arrange: UTCID10 - Account has no password (OAuth-only account)
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setPassword(null);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.login(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("login: should clear fail counter on successful login")
    void testLoginClearFailCounter() {
        // Arrange: UTCID11 - Successful login clears fail counter
        LoginRequest request = new LoginRequest();
        request.setEmail(testEmail);
        request.setPassword(testPassword);

        testAccount.setStatus(AccountStatus.ACTIVE);

        ValueOperations<String, String> mockOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(mockOps);
        when(mockOps.get(anyString())).thenReturn(null);

        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches(testPassword, testAccount.getPassword()))
                .thenReturn(true);
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("access_token");
        when(tokenFamilyService.createFamily(testAccountId))
                .thenReturn("refresh_token");

        // Act
        authService.login(request);

        // Assert: verify delete called
        verify(redisTemplate, times(1)).delete("login_fail:" + testEmail);
    }

    // ==================== GOOGLE LOGIN TESTS ====================

    @Test
    @DisplayName("googleLogin: should login existing Google account that is ACTIVE")
    void testGoogleLoginExistingActiveAccount() {
        // Arrange: UTCID01 - Existing account linked to Google, ACTIVE status
        String idToken = "valid_google_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_sub_123";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .name("Google User")
                .emailVerified(true)
                .pictureUrl("https://example.com/pic.jpg")
                .build();

        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setGoogleSubId(googleSubId);

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("access_token");
        when(tokenFamilyService.createFamily(testAccountId))
                .thenReturn("refresh_token");

        // Act
        GoogleAuthResponseDto response = authService.googleLogin(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        assertThat(response.getAccessToken()).isEqualTo("access_token");
        assertThat(response.getRefreshToken()).isEqualTo("refresh_token");
    }

    @Test
    @DisplayName("googleLogin: should return ONBOARDING status for new Google account")
    void testGoogleLoginNewAccount() {
        // Arrange: UTCID02 - New user via Google, creates account with ONBOARDING status
        String idToken = "valid_google_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_new_123";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email("newuser@example.com")
                .name("New Google User")
                .emailVerified(true)
                .pictureUrl("https://example.com/new.jpg")
                .build();

        Account newAccount = Account.builder()
                .accountId(UUID.randomUUID())
                .googleSubId(googleSubId)
                .email(googleInfo.getEmail())
                .status(AccountStatus.ONBOARDING)
                .role(viewerRole)
                .build();

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.empty());
        when(accountRepository.findByEmail(googleInfo.getEmail()))
                .thenReturn(Optional.empty());
        when(roleService.findByCode("VIEWER")).thenReturn(viewerRole);
        when(googleAccountCreationExecutor.createIsolated(any(Account.class)))
                .thenReturn(newAccount);
        when(jwtTokenProvider.generateVerificationToken(newAccount.getAccountId()))
                .thenReturn("verification_token");

        // Act
        GoogleAuthResponseDto response = authService.googleLogin(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ONBOARDING");
        assertThat(response.getVerificationToken()).isEqualTo("verification_token");
    }

    @Test
    @DisplayName("googleLogin: should link Google to existing email account")
    void testGoogleLoginLinkToExistingEmail() {
        // Arrange: UTCID03 - Google email matches existing account
        String idToken = "valid_google_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_link_123";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .name("Google User")
                .emailVerified(true)
                .pictureUrl("https://example.com/pic.jpg")
                .build();

        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setGoogleSubId(null); // Not yet linked

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.empty());
        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));
        when(accountRepository.save(any(Account.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("access_token");
        when(tokenFamilyService.createFamily(testAccountId))
                .thenReturn("refresh_token");

        // Act
        GoogleAuthResponseDto response = authService.googleLogin(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
        verify(accountRepository, times(1)).save(any(Account.class));
        verify(tokenFamilyService, times(1)).deleteAllFamilies(testAccountId);
    }

    @Test
    @DisplayName("googleLogin: should throw error when Google email not verified")
    void testGoogleLoginEmailNotVerified() {
        // Arrange: UTCID04 - Google email is not verified (security risk)
        String idToken = "unverified_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_unverified";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .emailVerified(false) // NOT verified
                .build();

        testAccount.setStatus(AccountStatus.ACTIVE);

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.empty());
        when(accountRepository.findByEmail(testEmail))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.googleLogin(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.GOOGLE_EMAIL_NOT_VERIFIED);
    }

    @Test
    @DisplayName("googleLogin: should throw ACCOUNT_BANNED for banned Google account")
    void testGoogleLoginBannedAccount() {
        // Arrange: UTCID05 - Existing Google account is BANNED
        String idToken = "banned_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_banned";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .emailVerified(true)
                .build();

        testAccount.setStatus(AccountStatus.BANNED);
        testAccount.setGoogleSubId(googleSubId);

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.googleLogin(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_BANNED);
    }

    @Test
    @DisplayName("googleLogin: should throw ACCOUNT_DELETED for deleted Google account")
    void testGoogleLoginDeletedAccount() {
        // Arrange: UTCID06 - Existing Google account is DELETED
        String idToken = "deleted_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_deleted";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .emailVerified(true)
                .build();

        testAccount.setStatus(AccountStatus.DELETED);
        testAccount.setGoogleSubId(googleSubId);

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.googleLogin(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_DELETED);
    }

    @Test
    @DisplayName("googleLogin: should return VERIFYING status for Google account in verification")
    void testGoogleLoginVerifyingAccount() {
        // Arrange: UTCID07 - Google account is VERIFYING
        String idToken = "verifying_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_verify";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .emailVerified(true)
                .build();

        testAccount.setStatus(AccountStatus.VERIFYING);
        testAccount.setGoogleSubId(googleSubId);

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateVerificationToken(testAccountId))
                .thenReturn("verification_token");

        // Act
        GoogleAuthResponseDto response = authService.googleLogin(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("VERIFYING");
        assertThat(response.getVerificationToken()).isEqualTo("verification_token");
        verify(otpService, times(1)).generateAndSend(testAccount);
    }

    @Test
    @DisplayName("googleLogin: should return ONBOARDING status for Google account requiring profile")
    void testGoogleLoginOnboardingAccount() {
        // Arrange: UTCID08 - Google account is ONBOARDING
        String idToken = "onboarding_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_onboard";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email(testEmail)
                .emailVerified(true)
                .build();

        testAccount.setStatus(AccountStatus.ONBOARDING);
        testAccount.setGoogleSubId(googleSubId);

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateVerificationToken(testAccountId))
                .thenReturn("verification_token");

        // Act
        GoogleAuthResponseDto response = authService.googleLogin(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ONBOARDING");
        assertThat(response.getVerificationToken()).isEqualTo("verification_token");
    }

    @Test
    @DisplayName("googleLogin: should handle race condition during new Google account creation")
    void testGoogleLoginConcurrentCreation() {
        // Arrange: UTCID09 - Race: two requests try to create same Google account
        String idToken = "race_token";
        GoogleLoginRequest request = new GoogleLoginRequest();
        request.setIdToken(idToken);

        String googleSubId = "google_race";
        GoogleUserInfo googleInfo = GoogleUserInfo.builder()
                .googleSubId(googleSubId)
                .email("race@example.com")
                .emailVerified(true)
                .build();

        Account existingAccount = Account.builder()
                .accountId(UUID.randomUUID())
                .googleSubId(googleSubId)
                .status(AccountStatus.ACTIVE)
                .build();

        when(googleAuthService.verifyIdToken(idToken)).thenReturn(googleInfo);
        when(accountRepository.findByGoogleSubId(googleSubId))
                .thenReturn(Optional.empty())
                .thenReturn(Optional.of(existingAccount)); // Second lookup after race
        when(accountRepository.findByEmail(googleInfo.getEmail()))
                .thenReturn(Optional.empty());
        when(googleAccountCreationExecutor.createIsolated(any(Account.class)))
                .thenThrow(new DataIntegrityViolationException("Duplicate key"));
        when(jwtTokenProvider.generateAccessToken(existingAccount))
                .thenReturn("access_token");
        when(tokenFamilyService.createFamily(existingAccount.getAccountId()))
                .thenReturn("refresh_token");

        // Act
        GoogleAuthResponseDto response = authService.googleLogin(request);

        // Assert: should resolve race gracefully
        assertThat(response).isNotNull();
        assertThat(response.getStatus()).isEqualTo("ACTIVE");
    }

    // ==================== REFRESH TOKEN TESTS ====================

    @Test
    @DisplayName("refreshToken: should return new tokens when refresh token is valid")
    void testRefreshTokenSuccess() {
        // Arrange: UTCID01 - Valid refresh token, valid account
        String oldRefreshToken = "old_refresh_token";
        String newRefreshToken = "new_refresh_token";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(oldRefreshToken);

        testAccount.setStatus(AccountStatus.ACTIVE);

        when(tokenFamilyService.validateAndRotate(oldRefreshToken))
                .thenReturn(newRefreshToken);
        when(tokenFamilyService.extractAccountId(newRefreshToken))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("new_access_token");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
    }

    @Test
    @DisplayName("refreshToken: should throw SESSION_EXPIRED when refresh token is invalid")
    void testRefreshTokenInvalid() {
        // Arrange: UTCID02 - Invalid/expired refresh token
        String invalidToken = "invalid_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(invalidToken);

        when(tokenFamilyService.validateAndRotate(invalidToken))
                .thenThrow(new AuthException(AuthErrorCode.SESSION_EXPIRED));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.SESSION_EXPIRED);
    }

    @Test
    @DisplayName("refreshToken: should throw error when account not found")
    void testRefreshTokenAccountNotFound() {
        // Arrange: UTCID03 - Refresh token valid but account deleted
        String refreshToken = "orphan_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn("new_token");
        when(tokenFamilyService.extractAccountId("new_token"))
                .thenReturn(UUID.randomUUID());
        when(accountRepository.findById(any(UUID.class)))
                .thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.SESSION_EXPIRED);
    }

    @Test
    @DisplayName("refreshToken: should throw error when account is VERIFYING")
    void testRefreshTokenVerifyingAccount() {
        // Arrange: UTCID04 - Account status is VERIFYING (not complete)
        String refreshToken = "verifying_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.VERIFYING);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn("new_token");
        when(tokenFamilyService.extractAccountId("new_token"))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_VERIFIED);
    }

    @Test
    @DisplayName("refreshToken: should throw error when account is ONBOARDING")
    void testRefreshTokenOnboardingAccount() {
        // Arrange: UTCID05 - Account status is ONBOARDING
        String refreshToken = "onboarding_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.ONBOARDING);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn("new_token");
        when(tokenFamilyService.extractAccountId("new_token"))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.PROFILE_INCOMPLETE);
    }

    @Test
    @DisplayName("refreshToken: should throw error when account is BANNED")
    void testRefreshTokenBannedAccount() {
        // Arrange: UTCID06 - Account status is BANNED
        String refreshToken = "banned_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.BANNED);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn("new_token");
        when(tokenFamilyService.extractAccountId("new_token"))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_BANNED);
    }

    @Test
    @DisplayName("refreshToken: should throw error when account is DELETED")
    void testRefreshTokenDeletedAccount() {
        // Arrange: UTCID07 - Account status is DELETED
        String refreshToken = "deleted_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.DELETED);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn("new_token");
        when(tokenFamilyService.extractAccountId("new_token"))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_DELETED);
    }

    @Test
    @DisplayName("refreshToken: should refresh token for ACTIVE account with avatar")
    void testRefreshTokenWithAvatar() {
        // Arrange: UTCID08 - ACTIVE account with avatar URL
        String refreshToken = "avatar_refresh";
        String newRefreshToken = "new_avatar_token";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.ACTIVE);
        testAccount.setAvatarUrl("https://example.com/avatar.jpg");

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn(newRefreshToken);
        when(tokenFamilyService.extractAccountId(newRefreshToken))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("avatar_access_token");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("avatar_access_token");
    }

    @Test
    @DisplayName("refreshToken: should handle token rotation")
    void testRefreshTokenRotation() {
        // Arrange: UTCID09 - Token family rotation mechanism
        String oldRefreshToken = "token_v1";
        String newRefreshToken = "token_v2";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(oldRefreshToken);

        testAccount.setStatus(AccountStatus.ACTIVE);

        when(tokenFamilyService.validateAndRotate(oldRefreshToken))
                .thenReturn(newRefreshToken);
        when(tokenFamilyService.extractAccountId(newRefreshToken))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("rotated_access_token");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
        verify(tokenFamilyService, times(1)).validateAndRotate(oldRefreshToken);
    }

    @Test
    @DisplayName("refreshToken: should reject double-use refresh token")
    void testRefreshTokenDoubleUse() {
        // Arrange: UTCID10 - Prevent reuse of old token (security)
        String refreshToken = "used_token";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenThrow(new AuthException(AuthErrorCode.SESSION_EXPIRED));

        // Act & Assert
        assertThatThrownBy(() -> authService.refreshToken(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.SESSION_EXPIRED);
    }

    @Test
    @DisplayName("refreshToken: should generate new access token with same account ID")
    void testRefreshTokenSameAccountId() {
        // Arrange: UTCID11 - Verify account ID stays consistent
        String refreshToken = "consistent_refresh";
        String newRefreshToken = "consistent_new_refresh";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.ACTIVE);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn(newRefreshToken);
        when(tokenFamilyService.extractAccountId(newRefreshToken))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("consistent_access");

        // Act
        authService.refreshToken(request);

        // Assert: verify account lookup
        verify(accountRepository, times(1)).findById(testAccountId);
    }

    @Test
    @DisplayName("refreshToken: should maintain refresh token chain for security")
    void testRefreshTokenChain() {
        // Arrange: UTCID12 - Token family chain security
        String refreshToken = "chain_v1";
        String newRefreshToken = "chain_v2";
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken(refreshToken);

        testAccount.setStatus(AccountStatus.ACTIVE);

        when(tokenFamilyService.validateAndRotate(refreshToken))
                .thenReturn(newRefreshToken);
        when(tokenFamilyService.extractAccountId(newRefreshToken))
                .thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId))
                .thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateAccessToken(testAccount))
                .thenReturn("chain_access");

        // Act
        AuthResponse response = authService.refreshToken(request);

        // Assert: old token replaced with new one
        assertThat(response.getRefreshToken()).isEqualTo(newRefreshToken);
        assertThat(response.getAccessToken()).isEqualTo("chain_access");
    }
}
