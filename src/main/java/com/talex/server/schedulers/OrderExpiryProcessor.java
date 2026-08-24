package com.talex.server.schedulers;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.campaign.CampaignWalletService;
import com.talex.server.services.coin.CoinWalletService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

/**
 * Xử lý đánh hết hạn 1 Order, tách riêng khỏi {@link OrderExpiryScheduler} để mỗi
 * order có transaction + pessimistic lock riêng (gọi method @Transactional từ
 * trong cùng class không tạo transaction mới qua Spring AOP proxy — self-invocation).
 * Lock trước, re-check status ngay dưới lock để tránh đè COMPLETED của webhook SePay
 * đang xử lý song song cho cùng order.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OrderExpiryProcessor {

    private final OrderRepository orderRepository;
    private final CoinWalletService coinWalletService;
    private final CampaignWalletService campaignWalletService;

    @Transactional
    public void expireIfStillAwaiting(String orderId) {
        Order order = orderRepository.findWithLockByOrderId(orderId).orElse(null);
        if (order == null) {
            return;
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.debug("Order {} no longer AWAITING_PAYMENT (status={}), skip expiry", orderId, order.getStatus());
            return;
        }

        order.setStatus(OrderStatus.OUT_OF_TIME);
        orderRepository.save(order);
        refundUnusedCoin(order);
        refundUnusedCampaignWallet(order);
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

    private void refundUnusedCampaignWallet(Order order) {
        if (order.getCampaignWalletAmount() != null
                && order.getCampaignWalletAmount().compareTo(BigDecimal.ZERO) > 0
        ) {
            campaignWalletService.creditWallet(order.getAccount().getAccountId(), order.getCampaignWalletAmount(),
                    "Hoàn tiền ví do hủy đơn hàng " + order.getPaymentCode(), order.getOrderId());
        }
    }
}
