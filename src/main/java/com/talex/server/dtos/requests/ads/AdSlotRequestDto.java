package com.talex.server.dtos.requests.ads;

import com.talex.server.enums.ads.AdSlotType;
import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;

@Data
public class AdSlotRequestDto {
    @NotBlank(message = "Code name is required")
    private String codeName;

    @NotBlank(message = "Display name is required")
    private String displayName;

    @NotNull(message = "Slot type is required")
    private AdSlotType type;

    @NotNull(message = "Price is required")
    @Min(value = 0, message = "Price cannot be negative")
    private Long price;

    @NotNull(message = "Total view of price is required")
    @Min(value = 1, message = "Total view must be at least 1")
    private Long totalViewOfPrice;

    private Boolean isActive = true;
}
