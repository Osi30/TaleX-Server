package com.talex.server.services;

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
import com.talex.server.dtos.responses.AuthResponse;

import java.util.UUID;

public interface AuthService {

    String register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyOtpRequest request);

    AuthResponse login(LoginRequest request);

    Object googleLogin(GoogleLoginRequest request);

    AuthResponse completeProfile(CompleteProfileRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    String resendOtp(ResendOtpRequest request);

    AccountProfileResponse getProfile(UUID accountId);

    AccountProfileResponse updateProfile(UUID accountId, UpdateProfileRequest request);

    void changePassword(UUID accountId, ChangePasswordRequest request);

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
