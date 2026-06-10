package com.talex.server.services;

import com.talex.server.entities.Account;

import java.util.UUID;

public interface OtpService {

    void generateAndSend(Account account);

    void verify(UUID accountId, String code);

    void enforceResendCooldown(UUID accountId);

    void generateAndSendPasswordReset(Account account);

    void verifyPasswordReset(UUID accountId, String code);

    void enforcePasswordResetCooldown(UUID accountId);
}
