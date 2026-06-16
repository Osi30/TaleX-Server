package com.talex.server.dtos.responses;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
public class AccountProfileResponse {
    private String accountId;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private LocalDate dateOfBirth;
    private String avatarUrl;
    private boolean hasPassword;
    private boolean googleLinked;
    private String roleName;
    private String status;
    private LocalDateTime createdAt;
}
