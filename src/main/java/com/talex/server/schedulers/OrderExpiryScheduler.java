package com.talex.server.schedulers;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.repositories.transaction.OrderRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;
    private final OrderExpiryProcessor orderExpiryProcessor;

    @Scheduled(
            fixedDelayString = "${payment.order.expiry-fixed-delay-ms:60000}",
            initialDelayString = "${payment.order.expiry-initial-delay-ms:60000}")
    public void expireDueOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> dueOrders = orderRepository
                .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(OrderStatus.AWAITING_PAYMENT, now);

        for (Order order : dueOrders) {
            try {
                orderExpiryProcessor.expireIfStillAwaiting(order.getOrderId());
            } catch (RuntimeException exception) {
                log.warn("Failed to expire order {}", order.getOrderId(), exception);
            }
        }
    }
}
