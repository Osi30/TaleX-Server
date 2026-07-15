package com.talex.server.services.payment.impls;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.configs.properties.SePayProperties;
import com.talex.server.dtos.requests.payment.CreateContentOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateEngagementOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateOrderRequestDto;
import com.talex.server.dtos.responses.payment.OrderResponseDto;
import com.talex.server.entities.Account;
import com.talex.server.entities.campaign.EngagementService;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.codes.PaymentErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.exceptions.details.PaymentException;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.coin.CoinPricingConverter;
import com.talex.server.services.coin.ICoinWalletService;
import com.talex.server.services.payment.IOrderService;
import com.talex.server.services.payment.ISePayService;
import com.talex.server.services.payment.OrderCompletionService;
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
    private final ICoinWalletService coinWalletService;
    private final CoinPricingConverter coinPricingConverter;
    private final OrderCompletionService orderCompletionService;
    private final ObjectMapper objectMapper;
    private final OrderExpirationMarker orderExpirationMarker;

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
        engagementOrderPreparationService.validateOwnedPublishedSeries(accountId, request.getSeriesIds());

        BigDecimal totalAmount = BigDecimal.valueOf(
                engagementService.getPrice() != null ? engagementService.getPrice() : 0L);
        String metadata = engagementOrderPreparationService.serializeSeriesIds(request.getSeriesIds());

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
        ContentOrderPreparationService.ContentPriceResolution priceResolution =
                contentOrderPreparationService.resolvePrice(accountId, itemType, request.getItemId());
        BigDecimal price = priceResolution.payablePrice();
        long coinAmountToUse = request.getCoinAmountToUse() != null ? request.getCoinAmountToUse() : 0L;

        Order order = resolveActiveOrCreateNew(accountId, itemType, request.getItemId(),
                () -> Order.builder()
                        .account(account)
                        .itemType(itemType)
                        .itemId(request.getItemId())
                        .totalAmount(price)
                        .fiatAmount(price)
                        .metadata(priceResolution.ownedEpisodeCount() > 0
                                ? serializeComboDiscount(priceResolution)
                                : null)
                        .build());

        // Tính lại theo CHÊNH LỆCH mỗi lần gọi (kể cả khi reuse order cũ) để slider Coin ở FE
        // luôn phản ánh đúng vào đơn — không trừ chồng vì chỉ debit/credit đúng phần thay đổi.
        if (order.getStatus() == OrderStatus.AWAITING_PAYMENT) {
            reconcileCoinPayment(accountId, order, coinAmountToUse);
        }

        return toResponseDto(order);
    }

    /**
     * Đưa số Coin áp dụng cho đơn hàng về đúng {@code coinAmountToUse} yêu cầu, bất kể đơn
     * là mới tạo hay đang reuse — chỉ debit/credit đúng phần CHÊNH LỆCH so với số Coin đã áp
     * trước đó cho chính đơn này, tránh trừ chồng khi client gọi lại API nhiều lần.
     * Không tự động hoàn tất đơn kể cả khi Coin đã đủ trả hết — user phải bấm xác nhận
     * riêng qua {@link #confirmCoinPayment(String, UUID)}.
     */
    private void reconcileCoinPayment(UUID accountId, Order order, long coinAmountToUse) {
        BigDecimal totalAmount = order.getTotalAmount();
        BigDecimal currentCoinApplied = BigDecimal.valueOf(
                order.getCoinAmount() != null ? order.getCoinAmount() : 0L);
        BigDecimal requestedCoin = BigDecimal.valueOf(Math.max(0, coinAmountToUse));

        // Coin có thể dùng = số dư hiện tại + số đã trừ cho chính đơn này (có thể hoàn lại)
        BigDecimal availableCoin = coinWalletService.getMyWallet(accountId).getBalance().add(currentCoinApplied);
        BigDecimal cappedRequestedCoin = requestedCoin.min(availableCoin);

        BigDecimal coinVndValue = coinPricingConverter.coinToVnd(cappedRequestedCoin).min(totalAmount);
        BigDecimal targetCoinToSpend = coinPricingConverter.vndToCoin(coinVndValue).min(cappedRequestedCoin);

        int comparison = targetCoinToSpend.compareTo(currentCoinApplied);
        if (comparison == 0) {
            return;
        } else if (comparison > 0) {
            BigDecimal delta = targetCoinToSpend.subtract(currentCoinApplied);
            coinWalletService.debitCoin(accountId, delta, CoinReferenceType.ORDER, order.getOrderId(),
                    "Thanh toán đơn hàng " + order.getPaymentCode());
        } else {
            BigDecimal delta = currentCoinApplied.subtract(targetCoinToSpend);
            coinWalletService.creditCoin(accountId, delta, CoinReferenceType.ORDER, order.getOrderId(),
                    "Hoàn Coin do điều chỉnh đơn hàng " + order.getPaymentCode());
        }

        BigDecimal fiatAmount = totalAmount.subtract(coinVndValue).max(BigDecimal.ZERO);
        order.setCoinAmount(targetCoinToSpend.longValueExact());
        order.setFiatAmount(fiatAmount);
        orderRepository.save(order);

        // KHÔNG tự động hoàn tất đơn dù Coin đã đủ trả hết — chỉ trừ/hoàn Coin theo lựa chọn
        // hiện tại của user. Việc hoàn tất đơn cần user bấm nút "Thanh toán" xác nhận riêng
        // (xem confirmCoinPayment), tránh trường hợp chỉ kéo thanh trượt đã bị trừ tiền/hoàn
        // tất đơn ngoài ý muốn.
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponseDto getOrder(String orderId, UUID accountId) {
        Order order = orderRepository.findByOrderIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));
        return toResponseDto(order);
    }

    @Override
    @Transactional
    public OrderResponseDto cancelOrder(String orderId, UUID accountId) {
        Order order = orderRepository.findByOrderIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_CANCELLABLE);
        }

        if (order.getCoinAmount() != null && order.getCoinAmount() > 0) {
            coinWalletService.creditCoin(accountId, BigDecimal.valueOf(order.getCoinAmount()),
                    CoinReferenceType.ORDER, order.getOrderId(),
                    "Hoàn Coin do hủy đơn hàng " + order.getPaymentCode());
        }

        order.setStatus(OrderStatus.CANCELLED);
        orderRepository.save(order);

        return toResponseDto(order);
    }

    /**
     * Xác nhận thanh toán hoàn toàn bằng Coin — yêu cầu user bấm nút "Thanh toán" rõ ràng,
     * chỉ hoàn tất khi Coin đã áp đủ trả hết đơn (fiatAmount = 0). Không tự động chạy khi
     * user chỉ kéo thanh trượt Coin.
     */
    @Override
    @Transactional
    public OrderResponseDto confirmCoinPayment(String orderId, UUID accountId) {
        Order order = orderRepository.findByOrderIdAndAccountId(orderId, accountId)
                .orElseThrow(() -> new PaymentException(PaymentErrorCode.ORDER_NOT_FOUND));

        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_CANCELLABLE);
        }

        if (order.getFiatAmount() == null || order.getFiatAmount().compareTo(BigDecimal.ZERO) != 0) {
            throw new PaymentException(PaymentErrorCode.ORDER_NOT_FULLY_COVERED_BY_COIN);
        }

        orderCompletionService.complete(order, order.getTotalAmount(), PaymentMethod.COIN);

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
            // Transaction riêng (REQUIRES_NEW) — phải commit thật, không được rollback
            // theo exception ném ra ngay sau đây, nếu không order kẹt AWAITING_PAYMENT mãi.
            orderExpirationMarker.markExpired(order);
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
        BigDecimal fiatAmount = order.getFiatAmount();
        boolean requiresOnlinePayment = fiatAmount != null && fiatAmount.compareTo(BigDecimal.ZERO) > 0;
        String qrUrl = requiresOnlinePayment
                ? sePayService.buildQrUrl(order.getPaymentCode(), fiatAmount)
                : null;

        ComboDiscountMetadata comboDiscount = ComboOrderFulfillmentService.ITEM_TYPE.equals(order.getItemType())
                ? parseComboDiscount(order.getMetadata())
                : null;

        return OrderResponseDto.builder()
                .orderId(order.getOrderId())
                .paymentCode(order.getPaymentCode())
                .qrUrl(qrUrl)
                .totalAmount(order.getTotalAmount())
                .coinAmountUsed(order.getCoinAmount())
                .fiatAmount(fiatAmount)
                .status(order.getStatus())
                .expiresAt(order.getExpiresAt())
                .comboOriginalPrice(comboDiscount != null ? comboDiscount.originalPrice() : null)
                .comboOwnedEpisodeCount(comboDiscount != null ? comboDiscount.ownedEpisodeCount() : null)
                .comboTotalEpisodeCount(comboDiscount != null ? comboDiscount.totalEpisodeCount() : null)
                .build();
    }

    /**
     * Lưu breakdown giảm giá combo (giá gốc, số tập đã sở hữu/tổng số tập) vào cột
     * {@code metadata} sẵn có của Order — chỉ ghi khi combo có giảm giá do sở hữu 1 phần,
     * để FE hiển thị minh bạch lý do giá thấp hơn giá niêm yết của combo.
     */
    private String serializeComboDiscount(ContentOrderPreparationService.ContentPriceResolution resolution) {
        try {
            return objectMapper.writeValueAsString(new ComboDiscountMetadata(
                    resolution.originalPrice(), resolution.ownedEpisodeCount(), resolution.totalEpisodeCount()));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Failed to serialize combo discount metadata", exception);
        }
    }

    private ComboDiscountMetadata parseComboDiscount(String metadata) {
        if (metadata == null || metadata.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(metadata, ComboDiscountMetadata.class);
        } catch (Exception exception) {
            return null;
        }
    }

    private record ComboDiscountMetadata(BigDecimal originalPrice, int ownedEpisodeCount, int totalEpisodeCount) {
    }
}
