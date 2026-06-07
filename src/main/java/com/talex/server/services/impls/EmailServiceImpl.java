package com.talex.server.services.impls;

import com.talex.server.services.EmailService;
import com.talex.server.utils.EmailTemplateUtil;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.MailSendException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailServiceImpl implements EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username}")
    private String fromEmail;

    @Override
    public void sendOtpEmail(String to, String otpCode) {
        try {
            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, "UTF-8");

            helper.setFrom(fromEmail);
            helper.setTo(to);
            helper.setSubject("TaleX - Mã xác thực tài khoản");
            helper.setText(EmailTemplateUtil.buildOtpEmailHtml(otpCode), true);

            mailSender.send(mimeMessage);
            log.info("Email sent to: {}", to);
        } catch (MailException | MessagingException e) {
            log.error("Email send failed to: {}", to, e);
            throw new MailSendException("Failed to send OTP email", e);
        }
    }

    @Override
    public void sendOtpEmailAsync(String to, String otpCode) {
        int maxRetries = 3;
        for (int attempt = 1; attempt <= maxRetries; attempt++) {
            try {
                sendOtpEmail(to, otpCode);
                return;
            } catch (Exception e) {
                log.warn("Async email attempt {}/{} failed for: {}", attempt, maxRetries, to, e);
                if (attempt == maxRetries) {
                    log.error("All {} email attempts failed for: {}", maxRetries, to, e);
                }
            }
        }
    }
}
