package com.talex.server.dtos.interaction.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AccountFollowInfoDto {
    private UUID accountId;
    private String username;
    private String avatarUrl;
    private LocalDateTime followedAt;
}