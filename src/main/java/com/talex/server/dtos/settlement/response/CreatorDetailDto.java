package com.talex.server.dtos.settlement.response;

import com.talex.server.enums.AccountStatus;
import com.talex.server.enums.creator.CreatorIdentityStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreatorDetailDto {
    private String creatorId;
    private Boolean isBanned;

    private String taxId;
    private CreatorIdentityStatus taxStatus;

    private UUID accountId;
    private String username;
    private String email;
    private AccountStatus accountStatus;
}