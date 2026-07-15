package com.talex.server.dtos.requests.admin;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CreateStaffRequestDto {

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 3, max = 30)
    private String username;

    @NotBlank
    @Size(min = 1, max = 100)
    private String fullName;

    @NotBlank
    @Size(min = 8, max = 100)
    private String password;
}
