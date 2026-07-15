package com.talex.server.dtos.requests.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;

    @NotBlank
    @Size(min = 1, max = 100)
    private String fullName;

    @NotNull
    @Past
    private LocalDate dateOfBirth;

    @NotBlank
    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Phone must be Vietnamese format: 0xxxxxxxxx or +84xxxxxxxxx")
    private String phone;
}
