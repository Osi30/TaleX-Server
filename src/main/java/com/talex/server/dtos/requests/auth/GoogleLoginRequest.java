package com.talex.server.dtos.requests.auth;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class GoogleLoginRequest {

    @NotBlank
    private String idToken;
}
