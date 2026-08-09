package com.talex.server.services.payment.impls;

import com.talex.server.entities.Notification;
import com.talex.server.entities.series.ComboEpisode;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.transaction.Order;
import com.talex.server.entities.transaction.Transaction;
import com.talex.server.enums.NotificationType;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.subscription.SubscriptionRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.invoice.IInvoiceService;
import com.talex.server.services.payment.IOrderFulfillmentService;
import com.talex.server.services.payment.ITransactionService;
import com.talex.server.services.payment.OrderCompletionService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class OrderCompletionServiceImpl implements OrderCompletionService {

    private final OrderRepository orderRepository;
    private final ITransactionService transactionService;
    private final IInvoiceService invoiceService;
    private final List<IOrderFulfillmentService> fulfillmentServices;
    private final NotificationRepository notificationRepository;
    private final SubscriptionRepository subscriptionRepository;
    private final ComboEpisodeRepository comboEpisodeRepository;
    private final EpisodeRepository episodeRepository;

    private Map<String, IOrderFulfillmentService> fulfillmentServiceByItemType;

    @PostConstruct
    void indexFulfillmentServices() {
        fulfillmentServiceByItemType = fulfillmentServices.stream()
                .collect(Collectors.toMap(IOrderFulfillmentService::getSupportedItemType, Function.identity()));
    }

    @Override
    @Transactional
    public void complete(Order order, BigDecimal paidAmount, PaymentMethod paymentMethod) {
        Transaction transaction = transactionService.createSuccessTransaction(order, paidAmount, paymentMethod);

        order.setStatus(OrderStatus.COMPLETED);
        orderRepository.save(order);

        IOrderFulfillmentService fulfillmentService = fulfillmentServiceByItemType.get(order.getItemType());
        if (fulfillmentService == null) {
            throw new IllegalStateException(
                    "No IOrderFulfillmentService registered for itemType=" + order.getItemType());
        }
        fulfillmentService.fulfill(order);

        log.info("Order {} completed via {}, itemType={} itemId={} fulfilled",
                order.getOrderId(), paymentMethod, order.getItemType(), order.getItemId());

        notifyPurchaseSuccess(order);

        // TẠM COMMENT: xuất hóa đơn điện tử qua SePay đang lỗi phía nhà cung cấp (matbao) —
        // request gửi đi đúng chuẩn, có tracking_code hợp lệ, nhưng bước xử lý bất đồng bộ
        // của SePay báo lỗi "thiếu template_code/invoice_series" dù 2 field này đã đúng và
        // đã verify trực tiếp qua API SePay. Tắt tạm để không tạo thêm Invoice PENDING vô
        // ích trong lúc chờ xác nhận với SePay support.
        // try {
        //     invoiceService.markPendingInvoice(order, transaction);
        // } catch (RuntimeException exception) {
        //     log.error("Failed to mark pending invoice for order {}", order.getOrderId(), exception);
        // }
    }

    private void notifyPurchaseSuccess(Order order) {
        String recipientAccountId = order.getAccount().getAccountId().toString();
        NotificationType type;
        String title;
        String content;
        String referenceType = null;
        String referenceId = null;

        if (SubscriptionOrderFulfillmentService.ITEM_TYPE.equals(order.getItemType())) {
            type = NotificationType.SUBSCRIPTION_PURCHASE_SUCCESS;
            String tierName = subscriptionRepository.findById(order.getItemId())
                    .map(Subscription::getTier)
                    .orElse("Premium");
            title = "Nâng cấp Premium thành công";
            content = "Bạn đã đăng ký gói " + tierName
                    + " thành công. Cảm ơn bạn đã đồng hành cùng TaleX, chúc bạn có trải nghiệm xem truyện tuyệt vời!";
        } else if (ComboOrderFulfillmentService.ITEM_TYPE.equals(order.getItemType())) {
            type = NotificationType.COMBO_PURCHASE_SUCCESS;
            String comboTitle = comboEpisodeRepository.findById(order.getItemId())
                    .map(ComboEpisode::getTitle)
                    .orElse("combo tập truyện");
            title = "Mua combo thành công";
            content = "Bạn đã mở khóa thành công combo \"" + comboTitle
                    + "\". Chúc bạn có trải nghiệm xem truyện tuyệt vời!";
            referenceType = "COMBO";
            referenceId = order.getItemId();
        } else if (EpisodeOrderFulfillmentService.ITEM_TYPE.equals(order.getItemType())) {
            type = NotificationType.EPISODE_PURCHASE_SUCCESS;
            String episodeTitle = episodeRepository.findById(order.getItemId())
                    .map(Episode::getTitle)
                    .orElse("tập truyện");
            title = "Mua tập truyện thành công";
            content = "Bạn đã mở khóa thành công tập \"" + episodeTitle
                    + "\". Chúc bạn có trải nghiệm xem truyện tuyệt vời!";
            referenceType = "EPISODE";
            referenceId = order.getItemId();
        } else {
            return;
        }

        notificationRepository.save(Notification.builder()
                .recipientId(recipientAccountId)
                .title(title)
                .content(content)
                .type(type)
                .referenceType(referenceType)
                .referenceId(referenceId)
                .build());
    }
}
