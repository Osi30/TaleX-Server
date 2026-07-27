package com.talex.server.dtos.responses.payment;

import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
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
public class OrderHistoryItemDto {
    private String orderId;
    private String itemType;
    private String itemTitle;
    private BigDecimal totalAmount;
    private PaymentMethod paymentMethod;
    private OrderStatus status;
    private LocalDateTime createdAt;
    private String invoiceUrl;
}
