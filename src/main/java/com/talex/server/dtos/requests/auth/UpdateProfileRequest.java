package com.talex.server.dtos.requests.auth;

import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.time.LocalDate;

@Data
public class UpdateProfileRequest {

    @Size(min = 3, max = 30)
    private String username;

    @Size(min = 1, max = 100)
    private String fullName;

    @Pattern(regexp = "^(0\\d{9}|\\+84\\d{9})$", message = "Số điện thoại không hợp lệ")
    private String phone;

    @Past
    private LocalDate dateOfBirth;

    private String avatarUrl;
}
