package com.talex.server.dtos.requests.coin;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * DTO nhận dữ liệu từ Admin khi cập nhật cấu hình kinh tế Coin.
 * <p>
 * Validation cơ bản tại tầng Controller (NotNull, Positive).
 * Validation logic nghiệp vụ (base < m7 < m14 < m30) thực hiện tại tầng Service.
 * </p>
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinEconomyConfigRequestDto {

    /** Thưởng cơ bản mỗi ngày điểm danh. */
    @NotNull(message = "Thưởng cơ bản không được để trống")
    @Positive(message = "Thưởng cơ bản phải lớn hơn 0")
    private BigDecimal dailyCheckInBase;

    /** Thưởng mốc 7 ngày liên tiếp. */
    @NotNull(message = "Thưởng mốc 7 ngày không được để trống")
    @Positive(message = "Thưởng mốc 7 ngày phải lớn hơn 0")
    private BigDecimal milestone7Reward;

    /** Thưởng mốc 14 ngày liên tiếp. */
    @NotNull(message = "Thưởng mốc 14 ngày không được để trống")
    @Positive(message = "Thưởng mốc 14 ngày phải lớn hơn 0")
    private BigDecimal milestone14Reward;

    /** Thưởng mốc 30 ngày liên tiếp. */
    @NotNull(message = "Thưởng mốc 30 ngày không được để trống")
    @Positive(message = "Thưởng mốc 30 ngày phải lớn hơn 0")
    private BigDecimal milestone30Reward;
}
