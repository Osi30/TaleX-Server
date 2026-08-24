package com.talex.server.dtos.responses.payment;

import com.talex.server.enums.transaction.OrderStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminOrderDetailDto {
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
    private Double vatRate;
    private BigDecimal vatAmount;
    private String metadata;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime expiresAt;
    private UUID buyerAccountId;
    private String buyerUsername;
    private String buyerEmail;
}
