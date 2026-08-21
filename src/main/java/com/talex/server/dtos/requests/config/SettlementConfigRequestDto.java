package com.talex.server.dtos.requests.config;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementConfigRequestDto {

    @NotNull(message = "Ngưỡng số dư tối thiểu không được để trống")
    @Min(value = 2000, message = "Ngưỡng số dư tối thiểu phải lớn hơn hoặc bằng 2000")
    @Max(value = 1000000000, message = "Ngưỡng số dư tối thiểu phải nhỏ hơn hoặc bằng 1000000000")
    private BigDecimal minBalanceThreshold;

    @NotNull(message = "Ngưỡng số dư tối thiểu không được để trống")
    @Min(value = 2000, message = "Ngưỡng số dư tối thiểu phải lớn hơn hoặc bằng 2000")
    @Max(value = 1000000000, message = "Ngưỡng số dư tối thiểu phải nhỏ hơn hoặc bằng 1000000000")
    private BigDecimal minPayoutThreshold;
}