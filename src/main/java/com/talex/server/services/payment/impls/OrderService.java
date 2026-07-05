package com.talex.server.services.payment.impls;

import com.talex.server.configs.properties.SePayProperties;
import com.talex.server.dtos.requests.payment.CreateContentOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateEngagementOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateOrderRequestDto;
import com.talex.server.dtos.responses.payment.OrderResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.exceptions.codes.PaymentErrorCode;
import com.talex.server.exceptions.details.PaymentException;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.payment.IOrderService;
import com.talex.server.services.payment.ISePayService;
import com.talex.server.services.subscription.ISubscriptionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

@Service
@RequiredArgsConstructor
public class OrderService implements IOrderService {

    private static final String ITEM_TYPE_SUBSCRIPTION = SubscriptionOrderFulfillmentService.ITEM_TYPE;
    private static final String ITEM_TYPE_ENGAGEMENT = EngagementOrderFulfillmentService.ITEM_TYPE;

    private final OrderRepository orderRepository;
    private final AccountRepository accountRepository;
    private final ISubscriptionService subscriptionService;
    private final ContentOrderPreparationService contentOrderPreparationService;
    private final EngagementOrderPreparationService engagementOrderPreparationService;
    private final ISePayService sePayService;
    private final SePayProperties sePayProperties;

    @Override
    @Transactional
    public OrderResponseDto createOrder(UUID accountId, CreateOrderRequestDto request) {
        Account account = fetchAccount(accountId);
        Subscription subscription = subscriptionService.getSubscriptionByIdEntity(request.getSubscriptionId());

        Order order = resolveActiveOrCreateNew(accountId, ITEM_TYPE_SUBSCRIPTION, subscription.getSubscriptionId(),
                () -> Order.builder()
                        .account(account)
                        .itemType(ITEM_TYPE_SUBSCRIPTION)
                        .itemId(subscription.getSubscriptionId())
                        .totalAmount(subscription.getPrice())
                        .fiatAmount(subscription.getPrice())
                        .build());

        return toResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto createEngagementOrder(UUID accountId, CreateEngagementOrderRequestDto request) {
        Account account = fetchAccount(accountId);
        EngagementService engagementService =
                engagementOrderPreparationService.fetchActiveEngagementService(request.getEngagementServiceId());
        engagementOrderPreparationService.validateOwnedPublishedEpisodes(accountId, request.getEpisodeIds());

        BigDecimal totalAmount = BigDecimal.valueOf(
                engagementService.getPrice() != null ? engagementService.getPrice() : 0L);
        String metadata = engagementOrderPreparationService.serializeEpisodeIds(request.getEpisodeIds());

        Order order = resolveActiveOrCreateNew(accountId, ITEM_TYPE_ENGAGEMENT, engagementService.getEngagementServiceId(),
                () -> Order.builder()
                        .account(account)
                        .itemType(ITEM_TYPE_ENGAGEMENT)
                        .itemId(engagementService.getEngagementServiceId())
                        .totalAmount(totalAmount)
                        .fiatAmount(totalAmount)
                        .metadata(metadata)
                        .build());

        return toResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto createContentOrder(UUID accountId, CreateContentOrderRequestDto request) {
        Account account = fetchAccount(accountId);
        String itemType = contentOrderPreparationService.normalizeItemType(request.getItemType());
        BigDecimal price = contentOrderPreparationService.resolvePrice(accountId, itemType, request.getItemId());

        Order order = resolveActiveOrCreateNew(accountId, itemType, request.getItemId(),
                () -> Order.builder()
                        .account(account)
                        .itemType(itemType)
                        .itemId(request.getItemId())
                        .totalAmount(price)
                        .fiatAmount(price)
                        .build());

        return toResponseDto(order);
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(String orderId, UUID accountId) {
        Order order = orderRepository.findByOrderIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));
        return toResponseDto(order);
    }

    private Order resolveActiveOrCreateNew(UUID accountId, String itemType, String itemId, Supplier<Order> factory) {
        Optional<Order> activeOrder = orderRepository
                .findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                        accountId, itemType, itemId, OrderStatus.AWAITING_PAYMENT);

        return activeOrder.isPresent()
                ? reuseOrBlockActiveOrder(activeOrder.get())
                : createNewOrder(factory);
    }

    private Order reuseOrBlockActiveOrder(Order order) {
        LocalDateTime now = LocalDateTime.now();
        Duration remaining = Duration.between(now, order.getExpiresAt());

        if (remaining.toMinutes() < sePayProperties.getRetryBlockWindowMinutes()) {
            order.setStatus(OrderStatus.OUT_OF_TIME);
            orderRepository.save(order);
            throw new PaymentException(PaymentErrorCode.ORDER_EXPIRED,
                    "Đơn hàng sắp hết hạn, vui lòng tạo đơn hàng mới");
        }

        return order;
    }

    private Order createNewOrder(Supplier<Order> factory) {
        LocalDateTime now = LocalDateTime.now();
        Order order = factory.get();
        order.setPaymentCode(generatePaymentCode());
        order.setCoinAmount(0L);
        order.setStatus(OrderStatus.AWAITING_PAYMENT);
        order.setExpiresAt(now.plusMinutes(sePayProperties.getOrderExpiryMinutes()));

        return orderRepository.save(order);
    }

    private String generatePaymentCode() {
        long sequence = orderRepository.nextPaymentCodeSequence();
        return "TLX" + String.format("%06d", sequence);
    }

    private Account fetchAccount(UUID accountId) {
        return accountRepository.findById(accountId)
                .orElseThrow(() -> new ResourceNotFoundException("Account not found with id: " + accountId));
    }

    private OrderResponseDto toResponseDto(Order order) {
        BigDecimal amount = order.getTotalAmount();
        String qrUrl = sePayService.buildQrUrl(order.getPaymentCode(), amount);

        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .paymentCode(order.getPaymentCode())
                .qrUrl(qrUrl)
                .totalAmount(amount)
                .status(order.getStatus())
                .expiresAt(order.getExpiresAt())
                .build();
    }
}
