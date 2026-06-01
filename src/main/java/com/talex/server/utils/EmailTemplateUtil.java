package com.talex.server.utils;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

public final class EmailTemplateUtil {

    private static final String OTP_TEMPLATE_PATH = "templates/email/otp-verification.html";
    private static final String OTP_PLACEHOLDER = "{{OTP_CODE}}";
    private static final String OTP_TEMPLATE = loadTemplate(OTP_TEMPLATE_PATH);

    private EmailTemplateUtil() {
    }

    public static String buildOtpEmailHtml(String otpCode) {
        if (otpCode == null || otpCode.isBlank()) {
            throw new IllegalArgumentException("OTP code must not be blank");
        }
        String spaced = String.join("  ", otpCode.split(""));
        return OTP_TEMPLATE.replace(OTP_PLACEHOLDER, spaced);
    }

    private static String loadTemplate(String path) {
        try (InputStream is = EmailTemplateUtil.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalStateException("Email template not found: " + path);
            }
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to load email template: " + path, e);
        }
    }
}
