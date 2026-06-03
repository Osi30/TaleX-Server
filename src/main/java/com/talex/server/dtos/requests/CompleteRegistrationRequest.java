package com.talex.server.dtos.requests;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

import java.time.LocalDate;

@Data
public class CompleteRegistrationRequest {

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Phone must be Vietnamese format: 0xxxxxxxxx or +84xxxxxxxxx")
    private String phone;
}
