package com.talex.server.mappers.impls;

import com.talex.server.dtos.responses.AuthResponse;
import com.talex.server.entities.Account;
import com.talex.server.mappers.AccountMapper;
import org.springframework.stereotype.Component;

@Component
public class AccountMapperImpl implements AccountMapper {

    @Override
    public AuthResponse toAuthResponse(Account account, String accessToken,
                                       String refreshToken, long expiresIn) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(expiresIn)
                .accountId(account.getAccountId())
                .username(account.getUsername())
                .email(account.getEmail())
                .role(account.getRole().getCode())
                .status(account.getStatus().name())
                .build();
    }
}
