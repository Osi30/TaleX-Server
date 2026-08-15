package com.talex.server.dtos.campaign.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EngagementServiceRequestDto {
    @NotBlank
    private String name;

    @NotBlank
    private String description;

    @Min(value = 1000, message = "Giá tối thiểu phải lớn hơn hoặc bằng 1000")
    @Max(value = 1000000, message = "Giá tối đa phải nhỏ hơn 1.000.000")
    private Long price;

    @NotNull
    private Boolean isActive;

    @Min(value = 1, message = "Mục tiêu tối thiểu phải lớn hơn 0")
    @Max(value = 1000, message = "Mục tiêu tối đa phải nhỏ hơn 1000")
    private Long targetValue;
}
