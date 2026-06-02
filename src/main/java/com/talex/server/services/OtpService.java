package com.talex.server.services;

import com.talex.server.entities.Account;

import java.util.UUID;

public interface OtpService {

    void generateAndSend(Account account);

    boolean verify(UUID accountId, String code);
}
