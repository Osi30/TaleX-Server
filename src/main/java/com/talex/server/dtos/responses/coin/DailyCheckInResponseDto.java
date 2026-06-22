package com.talex.server.dtos.responses.coin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

/**
 * Kết quả trả về sau khi điểm danh thành công.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCheckInResponseDto {

    /** Số coin được thưởng cho lần điểm danh này (đã tính streak multiplier). */
    private BigDecimal rewardAmount;

    /** Số ngày điểm danh liên tiếp hiện tại (sau khi điểm danh xong). */
    private Integer currentStreak;
}
