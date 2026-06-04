package com.talex.server.controllers;

import com.talex.server.dtos.requests.GoogleLoginRequest;
import com.talex.server.dtos.requests.LoginRequest;
import com.talex.server.dtos.requests.RefreshTokenRequest;
import com.talex.server.dtos.requests.RegisterRequest;
import com.talex.server.dtos.requests.ResendOtpRequest;
import com.talex.server.dtos.requests.VerifyOtpRequest;
import com.talex.server.dtos.responses.ApiResponse;
import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.services.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    @Operation(summary = "Login or register with Google OAuth2 ID token")
    public ResponseEntity<ApiResponse<?>> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
        Object result = authService.googleLogin(request);
        if (result instanceof AuthResponse auth) {
            return ResponseEntity.ok(ApiResponse.ok("Google login successful", auth));
        }
        return ResponseEntity.ok(ApiResponse.ok("OTP đã gửi tới email của bạn", (String) result));
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
}
