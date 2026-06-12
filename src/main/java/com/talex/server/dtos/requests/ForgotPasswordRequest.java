package com.talex.server.dtos.requests;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank
    @Email
    private String email;
}
