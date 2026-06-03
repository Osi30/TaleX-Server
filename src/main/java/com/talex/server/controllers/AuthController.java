package com.talex.server.controllers;

import com.talex.server.dtos.requests.CompleteRegistrationRequest;
import com.talex.server.dtos.requests.GoogleLoginRequest;
import com.talex.server.dtos.requests.LoginRequest;
import com.talex.server.dtos.requests.RefreshTokenRequest;
import com.talex.server.dtos.requests.RegisterRequest;
import com.talex.server.dtos.requests.ResendOtpRequest;
import com.talex.server.dtos.requests.VerifyOtpRequest;
import com.talex.server.dtos.responses.ApiResponse;
import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.exceptions.UnauthorizedException;
import com.talex.server.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
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
    @Operation(summary = "Register new account with email and password")
    public ResponseEntity<ApiResponse<Void>> register(@Valid @RequestBody RegisterRequest request) {
        String message = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(message));
    }

    @PostMapping("/verify-email")
    @Operation(summary = "Verify email with OTP code")
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
    @Operation(summary = "Login or register with Google OAuth2 ID token")
    public ResponseEntity<ApiResponse<AuthResponse>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        AuthResponse data = authService.googleLogin(request);
        return ResponseEntity.ok(ApiResponse.ok("Google login successful", data));
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
    @Operation(summary = "Resend OTP verification code to email")
    public ResponseEntity<ApiResponse<Void>> resendOtp(@Valid @RequestBody ResendOtpRequest request) {
        String message = authService.resendOtp(request);
        return ResponseEntity.ok(ApiResponse.ok(message));
    }

    @PostMapping("/complete-registration")
    @Operation(summary = "Complete registration for Google OAuth accounts (provide dateOfBirth and phone)")
    public ResponseEntity<ApiResponse<AuthResponse>> completeRegistration(
            Authentication authentication,
            @Valid @RequestBody CompleteRegistrationRequest request) {
        if (authentication == null || !authentication.isAuthenticated()) {
            throw new UnauthorizedException("Authentication required");
        }
        UUID accountId = UUID.fromString(authentication.getName());
        AuthResponse data = authService.completeRegistration(accountId, request);
        return ResponseEntity.ok(ApiResponse.ok("Registration completed successfully", data));
    }
}
