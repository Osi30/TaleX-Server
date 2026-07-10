package com.talex.server.dtos.responses.coin;

import com.talex.server.entities.coin.CoinEconomyConfig;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * DTO hiển thị cấu hình kinh tế Coin cho client / Admin dashboard.
 * Không expose các trường nhạy cảm của BaseAudit (deletedAt, deletedBy...).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CoinEconomyConfigResponseDto {

    /** ID của bản ghi cấu hình này. */
    private UUID configId;

    /** Thưởng cơ bản mỗi ngày điểm danh. */
    private BigDecimal dailyCheckInBase;

    /** Thưởng mốc 7 ngày liên tiếp. */
    private BigDecimal milestone7Reward;

    /** Thưởng mốc 14 ngày liên tiếp. */
    private BigDecimal milestone14Reward;

    /** Thưởng mốc 30 ngày liên tiếp. */
    private BigDecimal milestone30Reward;

    /** Tỷ giá quy đổi: 1 Coin tương đương bao nhiêu VNĐ. */
    private BigDecimal vndPerCoin;

    /** Thời điểm bản ghi này được tạo (= thời điểm Admin cập nhật). */
    private LocalDateTime createdAt;

    /** ID của Admin đã tạo cấu hình này (lấy từ createdBy của BaseAudit). */
    private String createdBy;

    /**
     * Factory method — map trực tiếp từ entity.
     *
     * @param config Entity CoinEconomyConfig (bản ghi mới nhất)
     * @return DTO an toàn để trả về client
     */
    public static CoinEconomyConfigResponseDto fromEntity(CoinEconomyConfig config) {
        return CoinEconomyConfigResponseDto.builder()
                .configId(config.getConfigId())
                .dailyCheckInBase(config.getDailyCheckInBase())
                .milestone7Reward(config.getMilestone7Reward())
                .milestone14Reward(config.getMilestone14Reward())
                .milestone30Reward(config.getMilestone30Reward())
                .vndPerCoin(config.getVndPerCoin())
                .createdAt(config.getCreatedAt())
                .createdBy(config.getCreatedBy())
                .build();
    }
}
