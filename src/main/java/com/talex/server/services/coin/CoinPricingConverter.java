package com.talex.server.services.coin;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

/**
 * Quy đổi VNĐ sang Coin và ngược lại theo tỷ giá {@code vndPerCoin} hiện hành
 * (đọc từ {@link ICoinEconomyConfigService}, đã có cache).
 * <p>
 * vndToCoin luôn làm tròn LÊN (ceil) để người dùng không bao giờ trả thiếu tiền đơn hàng.
 * </p>
 */
@Component
@RequiredArgsConstructor
public class CoinPricingConverter {

    private final ICoinEconomyConfigService coinEconomyConfigService;

    public BigDecimal vndToCoin(BigDecimal vnd) {
        BigDecimal vndPerCoin = currentRate();
        return vnd.divide(vndPerCoin, 0, RoundingMode.CEILING);
    }

    public BigDecimal coinToVnd(BigDecimal coin) {
        return coin.multiply(currentRate());
    }

    private BigDecimal currentRate() {
        return coinEconomyConfigService.getConfig().getVndPerCoin();
    }
}
