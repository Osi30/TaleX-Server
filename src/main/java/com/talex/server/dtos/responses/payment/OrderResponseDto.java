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
    private OrderStatus status;
    private LocalDateTime expiresAt;
}
