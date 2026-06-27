package com.talex.server.dtos.requests.creator;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorTierRequestDto {
    @NotBlank(message = "Tên tier không được để trống")
    private String tierName;

    @NotNull(message = "Cấp độ tier không được để trống")
    @Min(value = 0, message = "Cấp độ tier phải lớn hơn hoặc bằng 0")
    private Integer tierLevel;

    @NotNull(message = "Số follower yêu cầu không được để trống")
    @Min(value = 0, message = "Số follower phải lớn hơn hoặc bằng 0")
    private Long minFollowerRequired;

    @NotNull(message = "Số view yêu cầu không được để trống")
    @Min(value = 0, message = "Số view phải lớn hơn hoặc bằng 0")
    private Long minViewsRequired;

    @NotNull(message = "Thời gian xem yêu cầu không được để trống")
    @DecimalMin(value = "0.0", message = "Thời gian xem phải lớn hơn hoặc bằng 0")
    private Double minWatchTimeRequired;

    @NotNull(message = "Tỉ lệ chia quỹ Premium không được để trống")
    @DecimalMin(value = "0.0", message = "Tỉ lệ tối thiểu là 0.0 (0%)")
    @DecimalMax(value = "1.0", message = "Tỉ lệ tối đa là 1.0 (100%)")
    private Double premiumFundShareRatio;

    @NotNull(message = "Tỉ lệ chia doanh thu trực tiếp không được để trống")
    @DecimalMin(value = "0.0", message = "Tỉ lệ tối thiểu là 0.0 (0%)")
    @DecimalMax(value = "1.0", message = "Tỉ lệ tối đa là 1.0 (100%)")
    private Double directPurchaseShareRatio;

    @NotNull
    private Boolean isDefault;
}
