package com.talex.server.dtos.requests.creator;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorConfigRequestDto {
    @NotNull(message = "basePremiumShare không được để trống")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double basePremiumShare;

    @NotNull(message = "baseUnlockShare không được để trống")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double baseUnlockShare;
}