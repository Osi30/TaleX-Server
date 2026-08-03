package com.talex.server.services.auth;

import com.talex.server.dtos.requests.auth.ChangePasswordRequest;
import com.talex.server.dtos.requests.auth.CompleteProfileRequest;
import com.talex.server.dtos.requests.auth.ForgotPasswordRequest;
import com.talex.server.dtos.requests.auth.GoogleLoginRequest;
import com.talex.server.dtos.requests.auth.LoginRequest;
import com.talex.server.dtos.requests.auth.RefreshTokenRequest;
import com.talex.server.dtos.requests.auth.RegisterRequest;
import com.talex.server.dtos.requests.auth.ResendOtpRequest;
import com.talex.server.dtos.requests.auth.ResetPasswordRequest;
import com.talex.server.dtos.requests.auth.UpdateProfileRequest;
import com.talex.server.dtos.requests.auth.VerifyOtpRequest;
import com.talex.server.dtos.responses.auth.AccountProfileResponse;
import com.talex.server.dtos.responses.auth.AuthResponse;
import com.talex.server.dtos.responses.auth.GoogleAuthResponseDto;
import com.talex.server.entities.auth.Account;

import java.util.UUID;

public interface AuthService {

    String register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyOtpRequest request);

    AuthResponse login(LoginRequest request);

    GoogleAuthResponseDto googleLogin(GoogleLoginRequest request);

    AuthResponse completeProfile(CompleteProfileRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    String resendOtp(ResendOtpRequest request);

    AccountProfileResponse getProfile(UUID accountId);

    AccountProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void changePassword(UUID accountId, ChangePasswordRequest request);

    String forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);

    /** Validates account status and mints a fresh access/refresh token pair — used by the SSO handoff exchange. */
    AuthResponse issueTokensFor(Account account);
}
