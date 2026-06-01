package com.talex.server.services;

import com.talex.server.entities.Account;
import com.talex.server.entities.RefreshToken;

import java.util.UUID;

public interface RefreshTokenService {

    String createRefreshToken(Account account);

    RefreshToken findValidToken(String token);

    RefreshToken validateAndRotate(String token);

    void revokeAllByAccount(UUID accountId);
}
