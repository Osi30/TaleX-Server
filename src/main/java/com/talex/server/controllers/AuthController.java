package com.talex.server.controllers;

import com.talex.server.annotations.CurrentAccountId;
import com.talex.server.dtos.requests.ChangePasswordRequest;
import com.talex.server.dtos.requests.CompleteProfileRequest;
import com.talex.server.dtos.requests.ForgotPasswordRequest;
import com.talex.server.dtos.requests.GoogleLoginRequest;
import com.talex.server.dtos.requests.LoginRequest;
import com.talex.server.dtos.requests.RefreshTokenRequest;
import com.talex.server.dtos.requests.RegisterRequest;
import com.talex.server.dtos.requests.ResendOtpRequest;
import com.talex.server.dtos.requests.ResetPasswordRequest;
import com.talex.server.dtos.requests.UpdateProfileRequest;
import com.talex.server.dtos.requests.VerifyOtpRequest;
import com.talex.server.dtos.responses.AccountProfileResponse;
import com.talex.server.dtos.responses.ApiResponse;
import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.dtos.responses.GoogleAuthResponseDto;
import com.talex.server.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Register, Login, OTP verification, Google OAuth2, Token management")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    @Operation(summary = "Register new account — returns verification token")
    public ResponseEntity<ApiResponse<String>> register(@Valid @RequestBody RegisterRequest request) {
        String verificationToken = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok("OTP đã gửi tới email của bạn", verificationToken));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with verification token + OTP code")
    public ResponseEntity<ApiResponse<AuthResponse>> verifyEmail(@Valid @RequestBody VerifyOtpRequest request) {
        AuthResponse data = authService.verifyEmail(request);
        return ResponseEntity.ok(ApiResponse.ok("Email verified successfully", data));
    }

    @PostMapping("/login")
    @Operation(summary = "Login with email and password")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse data = authService.login(request);
        return ResponseEntity.ok(ApiResponse.ok("Login successful", data));
    }

    @PostMapping("/google")
    @Operation(summary = "Login or register with Google OAuth2 ID token — check 'status' field for next step")
    public ResponseEntity<ApiResponse<GoogleAuthResponseDto>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        GoogleAuthResponseDto result = authService.googleLogin(request);
        String message = "ACTIVE".equals(result.getStatus())
                ? "Google login successful"
                : "Vui lòng hoàn tất thông tin cá nhân";
        return ResponseEntity.ok(ApiResponse.ok(message, result));
    }

    @PostMapping("/complete-profile")
    @Operation(summary = "Complete profile after Google signup — provide phone and date of birth")
    public ResponseEntity<ApiResponse<AuthResponse>> completeProfile(@Valid @RequestBody CompleteProfileRequest request) {
        AuthResponse data = authService.completeProfile(request);
        return ResponseEntity.ok(ApiResponse.ok("Profile completed successfully", data));
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token using refresh token")
    public ResponseEntity<ApiResponse<AuthResponse>> refreshToken(@Valid @RequestBody RefreshTokenRequest request) {
        AuthResponse data = authService.refreshToken(request);
        return ResponseEntity.ok(ApiResponse.ok("Token refreshed", data));
    }

    @PostMapping("/logout")
    @Operation(summary = "Logout and revoke refresh tokens")
    public ResponseEntity<ApiResponse<Void>> logout(@Valid @RequestBody RefreshTokenRequest request) {
        authService.logout(request);
        return ResponseEntity.ok(ApiResponse.ok("Logged out successfully"));
    }

    @PostMapping("/resend-otp")
    @Operation(summary = "Resend OTP using verification token")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        String message = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    // ── Profile Management (Authenticated) ──────────────────────────

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Get current user profile")
    public ResponseEntity<ApiResponse<AccountProfileResponse>> getProfile(
            @CurrentAccountId UUID accountId) {
        AccountProfileResponse data = authService.getProfile(accountId);
        return ResponseEntity.ok(ApiResponse.ok("OK", data));
    }

    @PutMapping("/me")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Update current user profile")
    public ResponseEntity<ApiResponse<AccountProfileResponse>> updateProfile(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody UpdateProfileRequest request) {
        AccountProfileResponse data = authService.updateProfile(accountId, request);
        return ResponseEntity.ok(ApiResponse.ok("Cập nhật thông tin thành công", data));
    }

    @PostMapping("/change-password")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Change password — Google users can set password for the first time")
    public ResponseEntity<ApiResponse<Void>> changePassword(
            @CurrentAccountId UUID accountId,
            @Valid @RequestBody ChangePasswordRequest request) {
        authService.changePassword(accountId, request);
        return ResponseEntity.ok(ApiResponse.ok("Đổi mật khẩu thành công. Vui lòng đăng nhập lại."));
    }

    // ── Password Recovery (Public) ──────────────────────────────────

    @PostMapping("/forgot-password")
    @Operation(summary = "Request password reset OTP via email — returns verification token for reset flow")
    public ResponseEntity<ApiResponse<String>> forgotPassword(
            @Valid @RequestBody ForgotPasswordRequest request) {
        String verificationToken = authService.forgotPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Nếu email tồn tại, mã OTP đã được gửi tới email của bạn.",
                verificationToken));
    }

    @PostMapping("/reset-password")
    @Operation(summary = "Reset password with verification token + OTP")
    public ResponseEntity<ApiResponse<Void>> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(ApiResponse.ok(
                "Đặt lại mật khẩu thành công. Vui lòng đăng nhập lại."));
    }
}
