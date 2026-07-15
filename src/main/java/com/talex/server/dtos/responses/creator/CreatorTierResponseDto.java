package com.talex.server.dtos.responses.creator;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorTierResponseDto {
    private String creatorTierId;
    private String tierName;
    private Integer tierLevel;
    private Long minFollowerRequired;
    private Long minViewsRequired;
    private Double minWatchTimeRequired;
    private Double premiumFundShareRatio;
    private Double directPurchaseShareRatio;
    private Boolean isDefault;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime createdAt;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime updatedAt;
}
