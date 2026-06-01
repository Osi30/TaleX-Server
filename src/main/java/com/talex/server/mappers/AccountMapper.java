package com.talex.server.mappers;

import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.entities.Account;

public interface AccountMapper {

    AuthResponse toAuthResponse(Account account, String accessToken,
                                String refreshToken, long expiresIn);
}
