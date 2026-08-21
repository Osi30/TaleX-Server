package com.talex.server.dtos.requests.config;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxConfigRequestDto {
    @NotNull(message = "Thuế VAT không được để trống")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double vat;

    @NotNull(message = "Thuế PIT (TNCN) không được để trống")
    @DecimalMin(value = "0.0")
    @DecimalMax(value = "1.0")
    private Double pit;

    @NotNull(message = "Số tiền tối thiểu tính thuế PIT (TNCN) không được để trống")
    @Min(value = 0)
    private Long minPitAmount;
}