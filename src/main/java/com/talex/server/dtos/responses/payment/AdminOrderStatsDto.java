package com.talex.server.dtos.responses.payment;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderStatsDto {
    private Long totalOrders;
    // Key = OrderStatus.name() (AWAITING_PAYMENT/COMPLETED/CANCELLED/OUT_OF_TIME)
    private Map<String, Long> countByStatus;
    // Doanh thu đơn COMPLETED, gom nhóm theo itemType (SUBSCRIPTION/EPISODE/COMBO/ENGAGEMENT)
    private List<ItemTypeRevenue> revenueByItemType;
    private Double cancelledRatePercent;
    private Double expiredRatePercent;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ItemTypeRevenue {
        private String itemType;
        private BigDecimal totalRevenue;
        private Long orderCount;
    }
}
