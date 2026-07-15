package com.talex.server.dtos.responses.auth;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdminAccountResponseDto {
    private UUID accountId;
    private String email;
    private String username;
    private String fullName;
    private String avatarUrl;
    private String roleName;
    private String status;
    private LocalDateTime createdAt;
}
