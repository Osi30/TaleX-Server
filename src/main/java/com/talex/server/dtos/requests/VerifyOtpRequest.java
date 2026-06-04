package com.talex.server.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank
    private String verificationToken;

    @NotBlank
    private String otpCode;
}
