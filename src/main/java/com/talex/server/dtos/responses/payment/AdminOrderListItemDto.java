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
public class AdminOrderListItemDto {
    private String orderId;
    private String paymentCode;
    private OrderStatus status;
    private String itemType;
    private String itemId;
    private BigDecimal totalAmount;
    private Long coinAmount;
    private BigDecimal fiatAmount;
    private BigDecimal campaignWalletAmount;
    private BigDecimal overpaidAmount;
    private BigDecimal vatAmount;
    private LocalDateTime createdAt;
    private LocalDateTime expiresAt;
    private String buyerUsername;
    private String buyerEmail;
}
