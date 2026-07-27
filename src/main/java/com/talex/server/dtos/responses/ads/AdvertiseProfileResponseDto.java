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
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
