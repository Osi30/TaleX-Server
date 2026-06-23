package com.talex.server.dtos.responses.coin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Trạng thái điểm danh của user — dùng để frontend vẽ UI khi mở app.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyCheckInStatusDto {

    /** User đã điểm danh hôm nay chưa. */
    private Boolean isCheckedInToday;

    /**
     * Chuỗi ngày điểm danh hiện tại.
     * - Nếu hôm nay đã điểm danh: streak của hôm nay.
     * - Nếu chưa: streak của hôm qua (0 nếu hôm qua cũng không điểm danh).
     */
    private Integer currentStreak;
}
