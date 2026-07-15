package com.talex.server.dtos.responses.auth;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class GoogleAuthResponseDto {

    private String status;
    private String accessToken;
    private String refreshToken;
    private String verificationToken;
}
