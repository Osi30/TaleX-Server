package com.talex.server.services.auth;

import com.talex.server.entities.auth.Account;

import java.util.UUID;

public interface OtpService {

    void generateAndSend(Account account);

    void verify(UUID accountId, String code);

    void enforceResendCooldown(UUID accountId);

    void generateAndSendPasswordReset(Account account);

    void verifyPasswordReset(UUID accountId, String code);

    void enforcePasswordResetCooldown(UUID accountId);
}
