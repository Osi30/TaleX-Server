package com.talex.server.dtos.responses.ads;

import com.talex.server.enums.ads.AdSlotType;
import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdSlotResponseDto {
    private UUID slotId;
    private String codeName;
    private String displayName;
    private AdSlotType type;
    private Long price;
    private Long totalViewOfPrice;
    private Boolean isActive;
    private Boolean isServingEnabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
