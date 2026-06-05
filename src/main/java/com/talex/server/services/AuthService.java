package com.talex.server.services;

import com.talex.server.dtos.requests.GoogleLoginRequest;
import com.talex.server.dtos.requests.LoginRequest;
import com.talex.server.dtos.requests.RefreshTokenRequest;
import com.talex.server.dtos.requests.RegisterRequest;
import com.talex.server.dtos.requests.ResendOtpRequest;
import com.talex.server.dtos.requests.VerifyOtpRequest;
import com.talex.server.dtos.responses.AuthResponse;

public interface AuthService {

    String register(RegisterRequest request);

    AuthResponse verifyEmail(VerifyOtpRequest request);

    AuthResponse login(LoginRequest request);

    Object googleLogin(GoogleLoginRequest request);

    AuthResponse refreshToken(RefreshTokenRequest request);

    void logout(RefreshTokenRequest request);

    String resendOtp(ResendOtpRequest request);
}
