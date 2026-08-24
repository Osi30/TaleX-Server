package com.talex.server.services.payment.impls;

import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.OrderInterventionLog;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.transaction.OrderInterventionAction;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import com.talex.server.exceptions.details.payment.PaymentException;
import com.talex.server.repositories.transaction.OrderInterventionLogRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.campaign.CampaignWalletService;
import com.talex.server.services.coin.CoinWalletService;
import com.talex.server.services.payment.AdminOrderInterventionService;
import com.talex.server.services.payment.OrderCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdminOrderInterventionServiceImpl implements AdminOrderInterventionService {

    private final OrderRepository orderRepository;
    private final OrderInterventionLogRepository orderInterventionLogRepository;
    private final CoinWalletService coinWalletService;
    private final CampaignWalletService campaignWalletService;
    private final OrderCompletionService orderCompletionService;

    @Override
    @Transactional
    public Order cancelByAdmin(String orderId, UUID adminId, String reason) {
        Order order = orderRepository.findWithLockByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));

        guardIntervenable(order);
        String previousStatus = order.getStatus().name();
        UUID accountId = order.getAccount().getAccountId();

        // Hoàn trả Coin (nếu có) — copy logic từ OrderServiceImpl.cancelOrder(), admin cancel
        // khác user cancel duy nhất ở chỗ KHÔNG scope theo account khi tìm order.
        if (order.getCoinAmount() != null && order.getCoinAmount() > 0) {
            coinWalletService.creditCoin(accountId, BigDecimal.valueOf(order.getCoinAmount()),
                    CoinReferenceType.ORDER, order.getOrderId(),
                    "Hoàn Coin do Admin hủy đơn hàng " + order.getPaymentCode());
        }

        // Hoàn trả Campaign Wallet (nếu có)
        if (order.getCampaignWalletAmount() != null && order.getCampaignWalletAmount().compareTo(BigDecimal.ZERO) > 0) {
            campaignWalletService.creditWallet(accountId, order.getCampaignWalletAmount(),
                    "Hoàn tiền ví do Admin hủy đơn hàng " + order.getPaymentCode(), order.getOrderId());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        writeLog(order, OrderInterventionAction.CANCEL, previousStatus, OrderStatus.CANCELLED, adminId, reason);

        log.info("Admin {} cancelled order {} (previous status {})", adminId, orderId, previousStatus);
        return order;
    }

    @Override
    @Transactional
    public Order forceCompleteByAdmin(String orderId, UUID adminId, String reason) {
        Order order = orderRepository.findWithLockByOrderId(orderId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));

        guardIntervenable(order);
        String previousStatus = order.getStatus().name();

        // Ghi log TRƯỚC khi gọi complete() — nếu fulfill() bên trong ném lỗi, cả transaction
        // rollback (gồm log), order vẫn đứng nguyên trạng thái cũ, Admin retry được, không có
        // dữ liệu rác nào cần dọn (khác MediaPurgeLog phải ghi ngoài transaction vì có thao
        // tác S3 không rollback được — ở đây mọi side-effect đều trong cùng 1 DB transaction).
        writeLog(order, OrderInterventionAction.FORCE_COMPLETE, previousStatus, OrderStatus.COMPLETED, adminId, reason);

        BigDecimal fiatAmount = order.getFiatAmount();
        BigDecimal campaignWalletAmount = order.getCampaignWalletAmount();
        boolean noFiatDue = fiatAmount == null || fiatAmount.compareTo(BigDecimal.ZERO) == 0;

        if (noFiatDue && campaignWalletAmount != null && campaignWalletAmount.compareTo(order.getTotalAmount()) == 0) {
            // Đơn trả 100% bằng Campaign Wallet — dùng đúng đường completeViaWalletOnly (không
            // tạo Transaction fiat) mà OrderServiceImpl vốn dùng cho luồng tự động tương ứng.
            orderCompletionService.completeViaWalletOnly(order);
        } else if (noFiatDue) {
            // Đơn trả 100% bằng Coin — vẫn phải hoàn tất qua complete() để tạo Transaction audit,
            // nhưng ghi rõ ADMIN_MANUAL thay vì COIN để giữ dấu vết đây là can thiệp thủ công.
            orderCompletionService.complete(order, order.getTotalAmount(), PaymentMethod.ADMIN_MANUAL);
        } else {
            // Đơn còn phần fiat qua SePay chưa xác nhận được — paidAmount = fiatAmount, khớp
            // đúng semantic của SePayServiceImpl.handleWebhook() (truyền transferAmount nhận
            // được, không phải totalAmount gồm cả phần đã trả bằng Coin/Wallet).
            orderCompletionService.complete(order, fiatAmount, PaymentMethod.ADMIN_MANUAL);
        }

        log.info("Admin {} force-completed order {} (previous status {})", adminId, orderId, previousStatus);
        return order;
    }

    private void guardIntervenable(Order order) {
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT && order.getStatus() != OrderStatus.OUT_OF_TIME) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_INTERVENABLE);
        }
    }

    private void writeLog(Order order, OrderInterventionAction action, String previousStatus,
                           OrderStatus newStatus, UUID adminId, String reason) {
        orderInterventionLogRepository.save(OrderInterventionLog.builder()
                .orderId(order.getOrderId())
                .paymentCode(order.getPaymentCode())
                .action(action)
                .previousStatus(previousStatus)
                .newStatus(newStatus.name())
                .adminAccountId(adminId.toString())
                .reason(reason)
                .build());
    }
}
