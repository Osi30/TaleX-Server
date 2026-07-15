package com.talex.server.dtos.requests.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompleteProfileRequest {

    @NotBlank
    private String verificationToken;

    @NotNull
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không hợp lệ")
    private String phone;
}
