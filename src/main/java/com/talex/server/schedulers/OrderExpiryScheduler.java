package com.talex.server.schedulers;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.coin.ICoinWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class OrderExpiryScheduler {

    private final OrderRepository orderRepository;
    private final ICoinWalletService coinWalletService;

    @Scheduled(
            fixedDelayString = "${payment.order.expiry-fixed-delay-ms:60000}",
            initialDelayString = "${payment.order.expiry-initial-delay-ms:60000}")
    @Transactional
    public void expireDueOrders() {
        LocalDateTime now = LocalDateTime.now();
        List<Order> dueOrders = orderRepository
                .findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(OrderStatus.AWAITING_PAYMENT, now);

        for (Order order : dueOrders) {
            try {
                order.setStatus(OrderStatus.OUT_OF_TIME);
                orderRepository.save(order);
                refundUnusedCoin(order);
            } catch (RuntimeException exception) {
                log.warn("Failed to expire order {}", order.getOrderId(), exception);
            }
        }
    }

    private void refundUnusedCoin(Order order) {
        if (order.getCoinAmount() == null || order.getCoinAmount() <= 0) {
            return;
        }
        coinWalletService.creditCoin(
                order.getAccount().getAccountId(),
                BigDecimal.valueOf(order.getCoinAmount()),
                CoinReferenceType.ORDER,
                order.getOrderId(),
                "Hoàn Coin do đơn hàng hết hạn");
    }
}
