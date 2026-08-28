package com.talex.server.dtos.responses.ads;

import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdvertiseProfileResponseDto {
    private UUID profileId;
    private UUID accountId;
    private Long walletBalance;
    private String billingInfo;
    private String companyName;
    private String phone;
    private String website;
    private Boolean isSetupCompleted;
    private String username;
    private String email;
    private Boolean isLocked;
    private Integer campaignsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
