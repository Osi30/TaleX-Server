package com.talex.server.services.auth.impls;

import com.talex.server.configs.JwtTokenProvider;
import com.talex.server.dtos.requests.auth.ChangePasswordRequest;
import com.talex.server.dtos.requests.auth.ForgotPasswordRequest;
import com.talex.server.dtos.requests.auth.ResetPasswordRequest;
import com.talex.server.entities.auth.Account;
import com.talex.server.enums.AccountStatus;
import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.services.auth.OtpService;
import com.talex.server.services.auth.TokenFamilyService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountProfileServiceImpl Tests")
class AccountProfileServiceImplTest {

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtTokenProvider jwtTokenProvider;

    @Mock
    private OtpService otpService;

    @Mock
    private TokenFamilyService tokenFamilyService;

    @InjectMocks
    private AccountProfileServiceImpl service;

    private UUID testAccountId;
    private Account testAccount;
    private Account googleOnlyAccount;

    @BeforeEach
    void setUp() {
        testAccountId = UUID.randomUUID();

        // Regular account with password
        testAccount = Account.builder()
                .accountId(testAccountId)
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword123")
                .status(AccountStatus.ACTIVE)
                .build();

        // Google-only account (password is null)
        UUID googleAccountId = UUID.randomUUID();
        googleOnlyAccount = Account.builder()
                .accountId(googleAccountId)
                .email("google@example.com")
                .username("googleuser")
                .password(null)
                .status(AccountStatus.ACTIVE)
                .googleSubId("google-sub-123")
                .build();
    }

    // =====================================================================
    // changePassword() Tests — 13 test cases (UTCID01-UTCID13)
    // =====================================================================

    @Test
    @DisplayName("UTCID01: changePassword() thành công khi user có password, nhập currentPassword đúng, newPassword khác")
    void testChangePasswordSuccess_WithExistingPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("currentPass123", testAccount.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPass@123", testAccount.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.changePassword(testAccountId, request);

        // Assert — dùng literal "encodedPassword123" (KHÔNG phải testAccount.getPassword())
        // vì verify() re-evaluate expression tại THỜI ĐIỂM verify, không phải lúc gọi thật.
        // Service đã setPassword(encoded mới) lên CÙNG object testAccount trước khi verify()
        // chạy, nên testAccount.getPassword() lúc này đã là giá trị MỚI, không phải giá trị
        // cũ thực sự được truyền vào matches() lúc đó.
        verify(accountRepository, times(1)).findById(testAccountId);
        verify(passwordEncoder, times(1)).matches("currentPass123", "encodedPassword123");
        verify(passwordEncoder, times(1)).matches("newPass@123", "encodedPassword123");
        verify(passwordEncoder, times(1)).encode("newPass@123");
        verify(accountRepository, times(1)).save(argThat(account ->
                account.getPassword().equals("encodedNewPassword123")
        ));
        verify(tokenFamilyService, times(1)).deleteAllFamilies(testAccountId);
    }

    @Test
    @DisplayName("UTCID02: changePassword() thành công khi user là Google-only (password = null), không cần currentPassword")
    void testChangePasswordSuccess_GoogleOnlyUser() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(null);  // Google user không có password
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(googleOnlyAccount.getAccountId()))
                .thenReturn(Optional.of(googleOnlyAccount));
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.changePassword(googleOnlyAccount.getAccountId(), request);

        // Assert
        verify(accountRepository, times(1)).save(argThat(account ->
                account.getPassword().equals("encodedNewPassword123")
        ));
        verify(tokenFamilyService, times(1)).deleteAllFamilies(googleOnlyAccount.getAccountId());
    }

    @Test
    @DisplayName("UTCID03: changePassword() thất bại khi account không tồn tại")
    void testChangePasswordFail_AccountNotFound() {
        // Arrange
        UUID nonExistentId = UUID.randomUUID();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(nonExistentId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_CREDENTIALS);
    }

    @Test
    @DisplayName("UTCID04: changePassword() thất bại khi account không ACTIVE")
    void testChangePasswordFail_AccountNotActive() {
        // Arrange
        Account inactiveAccount = Account.builder()
                .accountId(testAccountId)
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword123")
                .status(AccountStatus.BANNED)
                .build();

        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(inactiveAccount));

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("UTCID05: changePassword() thất bại khi user có password nhưng currentPassword = null")
    void testChangePasswordFail_CurrentPasswordNull_UserHasPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(null);
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.CURRENT_PASSWORD_REQUIRED);
    }

    @Test
    @DisplayName("UTCID06: changePassword() thất bại khi user có password nhưng currentPassword = blank")
    void testChangePasswordFail_CurrentPasswordBlank_UserHasPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("   ");  // blank
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.CURRENT_PASSWORD_REQUIRED);
    }

    @Test
    @DisplayName("UTCID07: changePassword() thất bại khi currentPassword sai")
    void testChangePasswordFail_IncorrectCurrentPassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrongPassword");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("wrongPassword", testAccount.getPassword())).thenReturn(false);

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.CURRENT_PASSWORD_INCORRECT);
    }

    @Test
    @DisplayName("UTCID08: changePassword() thất bại khi newPassword trùng password cũ (hash match)")
    void testChangePasswordFail_NewPasswordSameAsOld() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("oldPassword123");
        request.setConfirmPassword("oldPassword123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("currentPass123", testAccount.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("oldPassword123", testAccount.getPassword())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.PASSWORD_SAME_AS_OLD);
    }

    @Test
    @DisplayName("UTCID09: changePassword() thất bại khi newPassword != confirmPassword")
    void testChangePasswordFail_PasswordMismatch() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("differentPass@123");

        // Không stub accountRepository.findById — validatePasswordConfirmation() chạy
        // TRƯỚC KHI fetch account trong code thật, nên account repo không hề được gọi tới.

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);
    }

    @Test
    @DisplayName("UTCID10: changePassword() xóa tất cả token families sau thay đổi password")
    void testChangePasswordRevokesAllSessions() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("currentPass123", testAccount.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPass@123", testAccount.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.changePassword(testAccountId, request);

        // Assert: verify deleteAllFamilies called (logout everywhere)
        verify(tokenFamilyService, times(1)).deleteAllFamilies(testAccountId);
    }

    @Test
    @DisplayName("UTCID11: changePassword() UPDATE password trong database")
    void testChangePasswordUpdatesDatabasePassword() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("currentPass123");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("currentPass123", testAccount.getPassword())).thenReturn(true);
        when(passwordEncoder.matches("newPass@123", testAccount.getPassword())).thenReturn(false);
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.changePassword(testAccountId, request);

        // Assert: verify password encoded and saved
        verify(passwordEncoder, times(1)).encode("newPass@123");
        verify(accountRepository, times(1)).save(testAccount);
    }

    @Test
    @DisplayName("UTCID12: changePassword() thất bại khi newPassword = currentPassword (plain text)")
    void testChangePasswordFail_NewPasswordEqualsCurrent() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("samePassword");
        request.setNewPassword("samePassword");
        request.setConfirmPassword("samePassword");

        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        when(passwordEncoder.matches("samePassword", testAccount.getPassword())).thenReturn(true);

        // Act & Assert
        assertThatThrownBy(() -> service.changePassword(testAccountId, request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.PASSWORD_SAME_AS_OLD);
    }

    @Test
    @DisplayName("UTCID13: changePassword() không call PasswordEncoder.matches nếu user là Google-only (password = null)")
    void testChangePasswordGoogleOnlySkipsCurrentPasswordValidation() {
        // Arrange
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword(null);
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(accountRepository.findById(googleOnlyAccount.getAccountId()))
                .thenReturn(Optional.of(googleOnlyAccount));
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.changePassword(googleOnlyAccount.getAccountId(), request);

        // Assert: matches() không được gọi vì password == null
        verify(passwordEncoder, never()).matches(anyString(), anyString());
        verify(passwordEncoder, times(1)).encode("newPass@123");
    }

    // =====================================================================
    // forgotPassword() Tests — 6 test cases (UTCID01-UTCID06)
    // =====================================================================

    @Test
    @DisplayName("UTCID01: forgotPassword() thành công khi email tồn tại, status ACTIVE, gửi OTP")
    void testForgotPasswordSuccess_ActiveAccount() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        String mockToken = "verification-token-123";
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateVerificationToken(testAccountId)).thenReturn(mockToken);

        // Act
        String result = service.forgotPassword(request);

        // Assert
        assertThat(result).isEqualTo(mockToken);
        verify(accountRepository, times(1)).findByEmail("test@example.com");
        verify(otpService, times(1)).enforcePasswordResetCooldown(testAccountId);
        verify(otpService, times(1)).generateAndSendPasswordReset(testAccount);
        verify(jwtTokenProvider, times(1)).generateVerificationToken(testAccountId);
    }

    @Test
    @DisplayName("UTCID02: forgotPassword() trả về dummy token khi email không tồn tại (anti-enumeration)")
    void testForgotPasswordReturnsRandomToken_EmailNotFound() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("nonexistent@example.com");

        UUID randomId = UUID.randomUUID();
        String dummyToken = "dummy-token-456";

        when(accountRepository.findByEmail("nonexistent@example.com")).thenReturn(Optional.empty());
        when(jwtTokenProvider.generateVerificationToken(any(UUID.class))).thenReturn(dummyToken);

        // Act
        String result = service.forgotPassword(request);

        // Assert: return dummy token, không call OTP service
        assertThat(result).isEqualTo(dummyToken);
        verify(jwtTokenProvider, times(1)).generateVerificationToken(any(UUID.class));
        verify(otpService, never()).enforcePasswordResetCooldown(any());
        verify(otpService, never()).generateAndSendPasswordReset(any());
    }

    @Test
    @DisplayName("UTCID03: forgotPassword() trả về dummy token khi account DEACTIVATED (không active)")
    void testForgotPasswordReturnsRandomToken_AccountInactive() {
        // Arrange
        Account inactiveAccount = Account.builder()
                .accountId(testAccountId)
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword123")
                .status(AccountStatus.BANNED)
                .build();

        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        String dummyToken = "dummy-token-789";

        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(inactiveAccount));
        when(jwtTokenProvider.generateVerificationToken(any(UUID.class))).thenReturn(dummyToken);

        // Act
        String result = service.forgotPassword(request);

        // Assert: return dummy token, không call OTP service
        assertThat(result).isEqualTo(dummyToken);
        verify(otpService, never()).enforcePasswordResetCooldown(any());
        verify(otpService, never()).generateAndSendPasswordReset(any());
    }

    @Test
    @DisplayName("UTCID04: forgotPassword() enforce password reset cooldown để ngăn spam")
    void testForgotPasswordEnforcesCooldown() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateVerificationToken(testAccountId)).thenReturn("token");
        doNothing().when(otpService).enforcePasswordResetCooldown(testAccountId);

        // Act
        service.forgotPassword(request);

        // Assert
        verify(otpService, times(1)).enforcePasswordResetCooldown(testAccountId);
    }

    @Test
    @DisplayName("UTCID05: forgotPassword() gửi OTP email cho account active")
    void testForgotPasswordSendsOTP() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateVerificationToken(testAccountId)).thenReturn("token");
        doNothing().when(otpService).generateAndSendPasswordReset(testAccount);

        // Act
        service.forgotPassword(request);

        // Assert
        verify(otpService, times(1)).generateAndSendPasswordReset(testAccount);
    }

    @Test
    @DisplayName("UTCID06: forgotPassword() trả về verification token chứa real accountId cho active user")
    void testForgotPasswordReturnsCorrectVerificationToken() {
        // Arrange
        ForgotPasswordRequest request = new ForgotPasswordRequest();
        request.setEmail("test@example.com");

        String correctToken = "real-verification-token-" + testAccountId;
        when(accountRepository.findByEmail("test@example.com")).thenReturn(Optional.of(testAccount));
        when(jwtTokenProvider.generateVerificationToken(testAccountId)).thenReturn(correctToken);

        // Act
        String result = service.forgotPassword(request);

        // Assert
        assertThat(result).isEqualTo(correctToken);
        verify(jwtTokenProvider, times(1)).generateVerificationToken(testAccountId);
    }

    // =====================================================================
    // resetPassword() Tests — 7 test cases (UTCID01-UTCID07)
    // =====================================================================

    @Test
    @DisplayName("UTCID01: resetPassword() thành công khi verification token hợp lệ, OTP đúng, account ACTIVE")
    void testResetPasswordSuccess() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("valid-token");
        request.setOtpCode("123456");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(jwtTokenProvider.extractVerificationAccountId("valid-token")).thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        doNothing().when(otpService).verifyPasswordReset(testAccountId, "123456");
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.resetPassword(request);

        // Assert
        verify(jwtTokenProvider, times(1)).extractVerificationAccountId("valid-token");
        verify(accountRepository, times(1)).findById(testAccountId);
        verify(otpService, times(1)).verifyPasswordReset(testAccountId, "123456");
        verify(passwordEncoder, times(1)).encode("newPass@123");
        verify(accountRepository, times(1)).save(testAccount);
        verify(tokenFamilyService, times(1)).deleteAllFamilies(testAccountId);
    }

    @Test
    @DisplayName("UTCID02: resetPassword() thất bại khi verification token không hợp lệ/expired")
    void testResetPasswordFail_InvalidToken() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("invalid-token");
        request.setOtpCode("123456");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        UUID invalidId = UUID.randomUUID();
        when(jwtTokenProvider.extractVerificationAccountId("invalid-token")).thenReturn(invalidId);
        when(accountRepository.findById(invalidId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.INVALID_VERIFICATION_TOKEN);
    }

    @Test
    @DisplayName("UTCID03: resetPassword() thất bại khi account không ACTIVE")
    void testResetPasswordFail_AccountNotActive() {
        // Arrange
        Account inactiveAccount = Account.builder()
                .accountId(testAccountId)
                .email("test@example.com")
                .username("testuser")
                .password("encodedPassword123")
                .status(AccountStatus.BANNED)
                .build();

        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("valid-token");
        request.setOtpCode("123456");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(jwtTokenProvider.extractVerificationAccountId("valid-token")).thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(inactiveAccount));

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.ACCOUNT_NOT_ACTIVE);
    }

    @Test
    @DisplayName("UTCID04: resetPassword() thất bại khi OTP sai")
    void testResetPasswordFail_InvalidOTP() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("valid-token");
        request.setOtpCode("wrong-otp");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(jwtTokenProvider.extractVerificationAccountId("valid-token")).thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        doThrow(new AuthException(AuthErrorCode.INVALID_OTP))
                .when(otpService).verifyPasswordReset(testAccountId, "wrong-otp");

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AuthException.class);
        verify(otpService, times(1)).verifyPasswordReset(testAccountId, "wrong-otp");
    }

    @Test
    @DisplayName("UTCID05: resetPassword() thất bại khi newPassword != confirmPassword")
    void testResetPasswordFail_PasswordMismatch() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("valid-token");
        request.setOtpCode("123456");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("differentPass@123");

        // Không stub jwtTokenProvider — validatePasswordConfirmation() chạy TRƯỚC KHI giải
        // mã verification token trong code thật.

        // Act & Assert
        assertThatThrownBy(() -> service.resetPassword(request))
                .isInstanceOf(AuthException.class)
                .extracting("errorCode")
                .isEqualTo(AuthErrorCode.PASSWORD_CONFIRMATION_MISMATCH);

        // Verify không gọi OTP verification nếu password mismatch
        verify(otpService, never()).verifyPasswordReset(any(), any());
    }

    @Test
    @DisplayName("UTCID06: resetPassword() xóa tất cả token families sau đặt lại password")
    void testResetPasswordRevokesAllSessions() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("valid-token");
        request.setOtpCode("123456");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(jwtTokenProvider.extractVerificationAccountId("valid-token")).thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        doNothing().when(otpService).verifyPasswordReset(testAccountId, "123456");
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.resetPassword(request);

        // Assert
        verify(tokenFamilyService, times(1)).deleteAllFamilies(testAccountId);
    }

    @Test
    @DisplayName("UTCID07: resetPassword() UPDATE password trong database")
    void testResetPasswordUpdatesDatabasePassword() {
        // Arrange
        ResetPasswordRequest request = new ResetPasswordRequest();
        request.setVerificationToken("valid-token");
        request.setOtpCode("123456");
        request.setNewPassword("newPass@123");
        request.setConfirmPassword("newPass@123");

        when(jwtTokenProvider.extractVerificationAccountId("valid-token")).thenReturn(testAccountId);
        when(accountRepository.findById(testAccountId)).thenReturn(Optional.of(testAccount));
        doNothing().when(otpService).verifyPasswordReset(testAccountId, "123456");
        when(passwordEncoder.encode("newPass@123")).thenReturn("encodedNewPassword123");

        // Act
        service.resetPassword(request);

        // Assert
        verify(passwordEncoder, times(1)).encode("newPass@123");
        verify(accountRepository, times(1)).save(testAccount);
    }
}
