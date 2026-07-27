package com.talex.server.dtos.requests.ads;

import lombok.Data;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Data
public class AdTopupRequestDto {
    @NotNull(message = "Amount is required")
    @Min(value = 10000, message = "Minimum topup amount is 10,000")
    private Long amount;
}
