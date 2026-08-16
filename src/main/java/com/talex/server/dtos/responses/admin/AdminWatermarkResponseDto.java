package com.talex.server.dtos.responses.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import com.talex.server.dtos.responses.auth.AdminAccountResponseDto;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminWatermarkResponseDto {
    private String creatorId;
    private String viewerId;
    private String message;
    private AdminAccountResponseDto creatorAccount;
    private AdminAccountResponseDto viewerAccount;
}
