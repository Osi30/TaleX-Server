package com.talex.server.dtos.requests.auth;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ChangePasswordRequest {

    // Nullable for Google-only users setting password for the first time
    private String currentPassword;

    @NotBlank
    @Size(min = 8, max = 100)
    private String newPassword;

    @NotBlank
    private String confirmPassword;
}
