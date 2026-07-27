package com.talex.server.services.auth;

public interface EmailService {

    void sendOtpEmail(String to, String otpCode);

    void sendOtpEmailAsync(String to, String otpCode);

    void sendPasswordResetEmail(String to, String otpCode);

    void sendPasswordResetEmailAsync(String to, String otpCode);

    void sendInvoiceEmail(String to, String invoicePdfUrl, byte[] invoicePdfBytes);

    void sendInvoiceEmailAsync(String to, String invoicePdfUrl, byte[] invoicePdfBytes);
}
