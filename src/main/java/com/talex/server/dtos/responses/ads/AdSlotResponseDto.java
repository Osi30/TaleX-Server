package com.talex.server.dtos.responses.ads;

import com.talex.server.enums.ads.AdSlotType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
public class AdSlotResponseDto {
    private UUID slotId;
    private String codeName;
    private String displayName;
    private AdSlotType type;
    private Long price;
    private Long totalViewOfPrice;
    private Boolean isActive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
