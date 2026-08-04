package com.talex.server.dtos.responses.auth;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SsoHandoffResponse {
    private String code;
    private long expiresIn;
}
