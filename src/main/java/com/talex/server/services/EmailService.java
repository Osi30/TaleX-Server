package com.talex.server.services;

public interface EmailService {

    void sendOtpEmail(String to, String otpCode);

    void sendOtpEmailAsync(String to, String otpCode);

    void sendPasswordResetEmail(String to, String otpCode);

    void sendPasswordResetEmailAsync(String to, String otpCode);
}
