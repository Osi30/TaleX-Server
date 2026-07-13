package com.talex.server.services.payment;

import com.talex.server.dtos.requests.payment.CreateContentOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateEngagementOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateOrderRequestDto;
import com.talex.server.dtos.responses.payment.OrderResponseDto;

import java.util.UUID;

public interface IOrderService {
    OrderResponseDto createOrder(UUID accountId, CreateOrderRequestDto request);

    OrderResponseDto createEngagementOrder(UUID accountId, CreateEngagementOrderRequestDto request);

    OrderResponseDto createContentOrder(UUID accountId, CreateContentOrderRequestDto request);

    OrderResponseDto getOrder(String orderId, UUID accountId);

    OrderResponseDto cancelOrder(String orderId, UUID accountId);

    OrderResponseDto confirmCoinPayment(String orderId, UUID accountId);
}
