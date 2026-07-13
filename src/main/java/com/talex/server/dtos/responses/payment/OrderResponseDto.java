package com.talex.server.dtos.responses.payment;

import com.talex.server.enums.transaction.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponseDto {
    private String orderId;
    private String paymentCode;
    private String qrUrl;
    private BigDecimal totalAmount;
    private Long coinAmountUsed;
    private BigDecimal fiatAmount;
    private OrderStatus status;
    private LocalDateTime expiresAt;
    // Chỉ có giá trị khi mua Combo và đã sở hữu 1 phần tập trong combo trước đó
    private BigDecimal comboOriginalPrice;
    private Integer comboOwnedEpisodeCount;
    private Integer comboTotalEpisodeCount;
}
