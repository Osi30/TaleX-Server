package com.talex.server.services;

import com.talex.server.entities.Account;

import java.util.UUID;

public interface OtpService {

    void generateAndSend(Account account);

    void verify(UUID accountId, String code);

    void enforceResendCooldown(UUID accountId);
}
