package com.talex.server.services.payment.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.configs.properties.SePayProperties;
import com.talex.server.dtos.requests.payment.CreateContentOrderRequestDto;
import com.talex.server.dtos.requests.payment.CreateOrderRequestDto;
import com.talex.server.dtos.responses.payment.OrderResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.coin.CoinWallet;
import com.talex.server.entities.config.TaxConfig;
import com.talex.server.entities.subscription.Subscription;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.exceptions.codes.payment.PaymentErrorCode;
import com.talex.server.exceptions.details.payment.PaymentException;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.campaign.CampaignService;
import com.talex.server.services.campaign.CampaignWalletService;
import com.talex.server.services.campaign.EngagementServiceService;
import com.talex.server.services.coin.CoinPricingConverter;
import com.talex.server.services.coin.CoinWalletService;
import com.talex.server.services.config.TaxConfigService;
import com.talex.server.services.payment.OrderCompletionService;
import com.talex.server.services.payment.SePayService;
import com.talex.server.services.subscription.SubscriptionService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderServiceImpl Tests")
class OrderServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private SubscriptionService subscriptionService;

    @Mock
    private ContentOrderPreparationServiceImpl contentOrderPreparationService;

    @Mock
    private SePayService sePayService;

    @Mock
    private SePayProperties sePayProperties;

    @Mock
    private CoinWalletService coinWalletService;

    @Mock
    private CoinPricingConverter coinPricingConverter;

    @Mock
    private OrderCompletionService orderCompletionService;

    @Mock
    private TaxConfigService taxConfigService;

    @Mock
    private CampaignWalletService campaignWalletService;

    @Mock
    private OrderExpirationMarker orderExpirationMarker;

    @Mock
    private EngagementServiceService engagementService;

    @Mock
    private CampaignService campaignService;

    // Spy với ObjectMapper THẬT (không mock) — code dùng để serialize metadata JSON thật
    // sự, mock rỗng sẽ trả null cho writeValueAsString() gây NPE. Test cần verify đúng
    // JSON output thật nên dùng instance thật là hợp lý hơn mock.
    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private UUID accountId;
    private Account testAccount;
    private TaxConfig testTaxConfig;
    private LocalDateTime now;

    @BeforeEach
    void setUp() {
        // objectMapper đã là @Spy khởi tạo sẵn ở field declaration — MockitoExtension init
        // mock/spy TRƯỚC @BeforeEach chạy, không cần (và không nên) gán lại ở đây.
        accountId = UUID.randomUUID();
        testAccount = Account.builder()
                .accountId(accountId)
                .username("testuser")
                .build();
        testTaxConfig = TaxConfig.builder()
                .vat(0.1) // 10% VAT
                .build();
        now = LocalDateTime.now();
    }

    // ==================== CreateOrder Tests (Premium Subscription) ====================

    @Nested
    @DisplayName("createOrder() - Premium Subscription Tests")
    class CreateOrderTests {

        private String subscriptionId;
        private Subscription testSubscription;

        @BeforeEach
        void setUp() {
            subscriptionId = "sub-001";
            testSubscription = Subscription.builder()
                    .subscriptionId(subscriptionId)
                    .price(BigDecimal.valueOf(99.99))
                    .build();
        }

        @Test
        @DisplayName("Prem_001_01: Tạo order Premium mới khi chưa có order nào")
        void testCreateOrderNewWhenNoActiveOrder() {
            // Arrange
            CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                    .subscriptionId(subscriptionId)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(subscriptionId)).thenReturn(testSubscription);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "SUBSCRIPTION", subscriptionId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);

            Order savedOrder = Order.builder()
                    .orderId("ord-001")
                    .account(testAccount)
                    .itemType("SUBSCRIPTION")
                    .itemId(subscriptionId)
                    .totalAmount(BigDecimal.valueOf(99.99))
                    .fiatAmount(BigDecimal.valueOf(99.99))
                    .paymentCode("TLX000001")
                    .coinAmount(0L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .expiresAt(now.plusMinutes(30))
                    .vatRate(0.1)
                    .vatAmount(BigDecimal.valueOf(9.09))
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);
            when(sePayService.buildQrUrl("TLX000001", BigDecimal.valueOf(99.99))).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createOrder(accountId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("ord-001");
            assertThat(response.getPaymentCode()).isEqualTo("TLX000001");
            assertThat(response.getTotalAmount()).isEqualTo(BigDecimal.valueOf(99.99));
            assertThat(response.getFiatAmount()).isEqualTo(BigDecimal.valueOf(99.99));
            assertThat(response.getCoinAmountUsed()).isEqualTo(0L);
            assertThat(response.getQrUrl()).isEqualTo("qr-url");

            verify(accountRepository, times(1)).findById(accountId);
            verify(subscriptionService, times(1)).getSubscriptionByIdEntity(subscriptionId);
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Prem_001_02: Tái sử dụng order cũ Premium khi còn hạn")
        void testCreateOrderReuseWhenActiveOrderExists() {
            // Arrange
            CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                    .subscriptionId(subscriptionId)
                    .build();

            Order existingOrder = Order.builder()
                    .orderId("ord-existing")
                    .account(testAccount)
                    .itemType("SUBSCRIPTION")
                    .itemId(subscriptionId)
                    .totalAmount(BigDecimal.valueOf(99.99))
                    .fiatAmount(BigDecimal.valueOf(99.99))
                    .paymentCode("TLX000001")
                    .coinAmount(0L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .expiresAt(now.plusMinutes(25)) // Còn 25 phút, > 15 phút (retry window)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(subscriptionId)).thenReturn(testSubscription);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "SUBSCRIPTION", subscriptionId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.of(existingOrder));
            when(sePayProperties.getRetryBlockWindowMinutes()).thenReturn(15);
            when(sePayService.buildQrUrl("TLX000001", BigDecimal.valueOf(99.99))).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createOrder(accountId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("ord-existing");
            assertThat(response.getPaymentCode()).isEqualTo("TLX000001");

            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Prem_001_03: Ném ORDER_EXPIRED khi order sắp hết hạn")
        void testCreateOrderThrowsWhenOrderExpired() {
            // Arrange
            CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                    .subscriptionId(subscriptionId)
                    .build();

            Order expiredOrder = Order.builder()
                    .orderId("ord-expired")
                    .account(testAccount)
                    .itemType("SUBSCRIPTION")
                    .itemId(subscriptionId)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .expiresAt(now.plusMinutes(10)) // Chỉ còn 10 phút, < 15 phút (retry window)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(subscriptionId)).thenReturn(testSubscription);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "SUBSCRIPTION", subscriptionId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.of(expiredOrder));
            when(sePayProperties.getRetryBlockWindowMinutes()).thenReturn(15);

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(accountId, request))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_EXPIRED);

            verify(orderExpirationMarker, times(1)).markExpired(expiredOrder);
        }

        @Test
        @DisplayName("Prem_001_04: Ném ResourceNotFoundException khi account không tồn tại")
        void testCreateOrderThrowsWhenAccountNotFound() {
            // Arrange
            CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                    .subscriptionId(subscriptionId)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createOrder(accountId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("Prem_001_05: Tính VAT đúng khi tạo order mới")
        void testCreateOrderCalculatesVatCorrectly() {
            // Arrange
            CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                    .subscriptionId(subscriptionId)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(subscriptionId)).thenReturn(testSubscription);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "SUBSCRIPTION", subscriptionId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order order = invocation.getArgument(0);
                order.setOrderId("ord-001");
                return order;
            });

            // Act
            orderService.createOrder(accountId, request);

            // Assert
            verify(orderRepository, times(1)).save(orderCaptor.capture());
            Order savedOrder = orderCaptor.getValue();

            assertThat(savedOrder.getVatRate()).isEqualTo(0.1);
            assertThat(savedOrder.getVatAmount()).isNotNull();
        }

        @Test
        @DisplayName("Prem_001_06: QR URL được tạo khi có fiatAmount > 0")
        void testCreateOrderGeneratesQrUrl() {
            // Arrange
            CreateOrderRequestDto request = CreateOrderRequestDto.builder()
                    .subscriptionId(subscriptionId)
                    .build();

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(subscriptionService.getSubscriptionByIdEntity(subscriptionId)).thenReturn(testSubscription);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "SUBSCRIPTION", subscriptionId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-001")
                    .account(testAccount)
                    .itemType("SUBSCRIPTION")
                    .itemId(subscriptionId)
                    .totalAmount(BigDecimal.valueOf(99.99))
                    .fiatAmount(BigDecimal.valueOf(99.99))
                    .paymentCode("TLX000001")
                    .coinAmount(0L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl("TLX000001", BigDecimal.valueOf(99.99))).thenReturn("qr-url-123");

            // Act
            OrderResponseDto response = orderService.createOrder(accountId, request);

            // Assert
            assertThat(response.getQrUrl()).isEqualTo("qr-url-123");
            verify(sePayService, times(1)).buildQrUrl("TLX000001", BigDecimal.valueOf(99.99));
        }
    }

    // ==================== CancelOrder Tests (Premium & Combo) ====================

    @Nested
    @DisplayName("cancelOrder() - Premium & Combo Tests")
    class CancelOrderTests {

        private String orderId;

        @BeforeEach
        void setUp() {
            orderId = "ord-to-cancel";
        }

        @Test
        @DisplayName("Prem_002_01: Hủy order Premium thành công, hoàn toàn bộ Coin")
        void testCancelPremiumOrderSuccessWithCoin() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000001")
                    .coinAmount(100L)
                    .totalAmount(BigDecimal.valueOf(99.99))
                    .fiatAmount(BigDecimal.valueOf(50.00))
                    .itemType("SUBSCRIPTION")
                    .itemId("sub-001")
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act
            OrderResponseDto response = orderService.cancelOrder(orderId, accountId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            verify(coinWalletService, times(1))
                    .creditCoin(accountId, BigDecimal.valueOf(100L),
                            CoinReferenceType.ORDER, orderId,
                            "Hoàn Coin do hủy đơn hàng TLX000001");
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Prem_002_02: Hủy order Combo thành công, hoàn Coin + Campaign Wallet")
        void testCancelComboOrderSuccessWithCoinAndWallet() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000002")
                    .coinAmount(50L)
                    .campaignWalletAmount(BigDecimal.valueOf(25.00))
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .fiatAmount(BigDecimal.valueOf(25.00))
                    .itemType("COMBO")
                    .itemId("combo-001")
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act
            OrderResponseDto response = orderService.cancelOrder(orderId, accountId);

            // Assert
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);

            verify(coinWalletService, times(1))
                    .creditCoin(accountId, BigDecimal.valueOf(50L),
                            CoinReferenceType.ORDER, orderId,
                            "Hoàn Coin do hủy đơn hàng TLX000002");

            verify(campaignWalletService, times(1))
                    .creditWallet(accountId, BigDecimal.valueOf(25.00),
                            "Hoàn tiền ví do hủy đơn hàng TLX000002", orderId);
        }

        @Test
        @DisplayName("Prem_002_03: Ném ORDER_NOT_CANCELLABLE khi order không ở trạng thái AWAITING_PAYMENT")
        void testCancelOrderThrowsWhenNotAwaitingPayment() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.COMPLETED)
                    .paymentCode("TLX000001")
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, accountId))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_NOT_CANCELLABLE);

            verify(coinWalletService, never()).creditCoin(any(), any(), any(), any(), any());
            verify(campaignWalletService, never()).creditWallet(any(), any(), any(), any());
        }

        @Test
        @DisplayName("Prem_002_04: Ném ORDER_NOT_FOUND khi order không tồn tại")
        void testCancelOrderThrowsWhenNotFound() {
            // Arrange
            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.cancelOrder(orderId, accountId))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_NOT_FOUND);
        }

        @Test
        @DisplayName("Prem_002_05: Không hoàn Coin khi coinAmount = null hoặc 0")
        void testCancelOrderNoCoinWhenCoinAmountIsNull() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000001")
                    .coinAmount(null)
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act
            orderService.cancelOrder(orderId, accountId);

            // Assert
            verify(coinWalletService, never()).creditCoin(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Combo_006_01: Hủy order Episode/Combo thành công")
        void testCancelComboOrderSuccess() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000003")
                    .coinAmount(75L)
                    .totalAmount(BigDecimal.valueOf(150.00))
                    .itemType("COMBO")
                    .itemId("combo-123")
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act
            OrderResponseDto response = orderService.cancelOrder(orderId, accountId);

            // Assert
            assertThat(response.getStatus()).isEqualTo(OrderStatus.CANCELLED);
            verify(coinWalletService, times(1)).creditCoin(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Prem_002_06: Không hoàn Campaign Wallet khi campaignWalletAmount = null hoặc 0")
        void testCancelOrderNoCampaignWalletWhenNull() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000001")
                    .coinAmount(50L)
                    .campaignWalletAmount(BigDecimal.ZERO)
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act
            orderService.cancelOrder(orderId, accountId);

            // Assert
            verify(campaignWalletService, never()).creditWallet(any(), any(), any(), any());
            verify(coinWalletService, times(1)).creditCoin(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Prem_002_07: Order status thay đổi thành CANCELLED sau hủy")
        void testCancelOrderStatusChanged() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000001")
                    .coinAmount(0L)
                    .build();

            when(orderRepository.findByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order arg = invocation.getArgument(0);
                arg.setOrderId(orderId);
                return arg;
            });

            // Act
            orderService.cancelOrder(orderId, accountId);

            // Assert
            verify(orderRepository, times(1)).save(orderCaptor.capture());
            assertThat(orderCaptor.getValue().getStatus()).isEqualTo(OrderStatus.CANCELLED);
        }
    }

    // ==================== CreateContentOrder Tests (Combo/Episode) ====================

    @Nested
    @DisplayName("createContentOrder() - Combo/Episode Tests")
    class CreateContentOrderTests {

        private String itemId;
        private CreateContentOrderRequestDto request;

        @BeforeEach
        void setUp() {
            itemId = "combo-001";
            request = CreateContentOrderRequestDto.builder()
                    .itemType("COMBO")
                    .itemId(itemId)
                    .coinAmountToUse(0L)
                    .build();

            // reconcileCoinPayment() luôn gọi getMyWallet() để tính availableCoin, kể cả khi
            // coinAmountToUse=0 (cần cộng dồn currentCoinApplied) — mock default balance=0,
            // lenient vì không phải test nào cũng đi tới nhánh AWAITING_PAYMENT reconcile.
            lenient().when(coinWalletService.getMyWallet(accountId))
                    .thenReturn(CoinWallet.builder().balance(BigDecimal.ZERO).build());
            lenient().when(coinPricingConverter.coinToVnd(any(BigDecimal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
            lenient().when(coinPricingConverter.vndToCoin(any(BigDecimal.class)))
                    .thenAnswer(inv -> inv.getArgument(0));
        }

        @Test
        @DisplayName("Combo_001_01: Tạo order Combo mới khi không có owned episodes")
        void testCreateContentOrderNewNoDiscount() {
            // Arrange
            BigDecimal comboPrice = BigDecimal.valueOf(200.00);

            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            comboPrice, comboPrice, 0, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("COMBO")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-combo-001")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .totalAmount(comboPrice)
                    .fiatAmount(comboPrice)
                    .paymentCode("TLX000001")
                    .coinAmount(0L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl("TLX000001", comboPrice)).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createContentOrder(accountId, request);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo("ord-combo-001");
            assertThat(response.getTotalAmount()).isEqualTo(comboPrice);
            assertThat(response.getFiatAmount()).isEqualTo(comboPrice);

            verify(accountRepository, times(1)).findById(accountId);
            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Combo_001_02: Tạo order Combo với discount khi đã sở hữu một số episodes")
        void testCreateContentOrderWithDiscount() {
            // Arrange
            BigDecimal originalPrice = BigDecimal.valueOf(200.00);
            BigDecimal discountedPrice = BigDecimal.valueOf(150.00);

            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            discountedPrice, originalPrice, 3, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("COMBO")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-combo-002")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .totalAmount(discountedPrice)
                    .fiatAmount(discountedPrice)
                    .paymentCode("TLX000002")
                    .coinAmount(0L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .metadata("{\"originalPrice\":200.00,\"ownedEpisodeCount\":3,\"totalEpisodeCount\":10}")
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl("TLX000002", discountedPrice)).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createContentOrder(accountId, request);

            // Assert
            assertThat(response.getTotalAmount()).isEqualTo(discountedPrice);
            assertThat(response.getComboOwnedEpisodeCount()).isEqualTo(3);
            assertThat(response.getComboTotalEpisodeCount()).isEqualTo(10);

            verify(orderRepository, times(1)).save(any(Order.class));
        }

        @Test
        @DisplayName("Combo_001_03: Tái sử dụng order Combo cũ khi còn hạn")
        void testCreateContentOrderReuseExisting() {
            // Arrange
            Order existingOrder = Order.builder()
                    .orderId("ord-combo-existing")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .expiresAt(now.plusMinutes(25))
                    // reconcileCoinPayment() (chạy vì order status = AWAITING_PAYMENT) cần
                    // totalAmount non-null để .min(totalAmount) không NPE.
                    .totalAmount(BigDecimal.valueOf(200.00))
                    .fiatAmount(BigDecimal.valueOf(200.00))
                    .coinAmount(0L)
                    .build();

            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            BigDecimal.valueOf(200.00), BigDecimal.valueOf(200.00), 0, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("COMBO")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.of(existingOrder));
            when(sePayProperties.getRetryBlockWindowMinutes()).thenReturn(15);
            when(sePayService.buildQrUrl(any(), any())).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createContentOrder(accountId, request);

            // Assert
            assertThat(response.getOrderId()).isEqualTo("ord-combo-existing");
            verify(orderRepository, never()).save(any(Order.class));
        }

        @Test
        @DisplayName("Combo_001_04: Không áp dụng Coin khi coinAmountToUse = null")
        void testCreateContentOrderNoCoinWhenNull() {
            // Arrange
            request.setCoinAmountToUse(null);

            BigDecimal comboPrice = BigDecimal.valueOf(200.00);
            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            comboPrice, comboPrice, 0, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("COMBO")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-combo-no-coin")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .totalAmount(comboPrice)
                    .fiatAmount(comboPrice)
                    .coinAmount(0L)
                    .paymentCode("TLX000001")
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl("TLX000001", comboPrice)).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createContentOrder(accountId, request);

            // Assert
            assertThat(response.getCoinAmountUsed()).isEqualTo(0L);
            verify(coinWalletService, never()).debitCoin(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Combo_001_05: Ném exception khi account không tồn tại")
        void testCreateContentOrderThrowsWhenAccountNotFound() {
            // Arrange
            when(accountRepository.findById(accountId)).thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.createContentOrder(accountId, request))
                    .isInstanceOf(ResourceNotFoundException.class)
                    .hasMessageContaining("Account not found");
        }

        @Test
        @DisplayName("Combo_001_06: Metadata serialization khi có discount")
        void testCreateContentOrderSerializesMetadata() {
            // Arrange
            BigDecimal originalPrice = BigDecimal.valueOf(200.00);
            BigDecimal discountedPrice = BigDecimal.valueOf(150.00);

            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            discountedPrice, originalPrice, 3, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("COMBO")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
            Order savedOrder = Order.builder()
                    .orderId("ord-metadata")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .totalAmount(discountedPrice)
                    .fiatAmount(discountedPrice)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
                Order arg = invocation.getArgument(0);
                arg.setOrderId("ord-metadata");
                return arg;
            });
            when(sePayService.buildQrUrl(any(), any())).thenReturn("qr-url");

            // Act
            orderService.createContentOrder(accountId, request);

            // Assert
            verify(orderRepository, times(1)).save(orderCaptor.capture());
            Order captured = orderCaptor.getValue();

            assertThat(captured.getMetadata()).isNotNull();
            assertThat(captured.getMetadata()).contains("originalPrice");
            assertThat(captured.getMetadata()).contains("ownedEpisodeCount");
        }

        @Test
        @DisplayName("Combo_001_07: Episode không có discount nếu chưa sở hữu")
        void testCreateContentOrderEpisodeNoDiscount() {
            // Arrange
            request.setItemType("EPISODE");
            request.setItemId("ep-001");

            BigDecimal episodePrice = BigDecimal.valueOf(29.99);
            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            episodePrice, episodePrice, 0, 1);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("EPISODE")).thenReturn("EPISODE");
            when(contentOrderPreparationService.resolvePrice(accountId, "EPISODE", "ep-001"))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "EPISODE", "ep-001", OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-episode")
                    .account(testAccount)
                    .itemType("EPISODE")
                    .itemId("ep-001")
                    .totalAmount(episodePrice)
                    .fiatAmount(episodePrice)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl(any(), any())).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createContentOrder(accountId, request);

            // Assert
            assertThat(response.getTotalAmount()).isEqualTo(episodePrice);
            assertThat(response.getComboOwnedEpisodeCount()).isNull();
        }

        @Test
        @DisplayName("Combo_001_08: Không gọi reconcileCoinPayment khi order đã COMPLETED")
        void testCreateContentOrderSkipsReconcileWhenCompleted() {
            // Arrange
            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            BigDecimal.valueOf(200.00), BigDecimal.valueOf(200.00), 0, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("COMBO")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-new")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .totalAmount(BigDecimal.valueOf(200.00))
                    .fiatAmount(BigDecimal.valueOf(200.00))
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl(any(), any())).thenReturn("qr-url");

            // Act
            orderService.createContentOrder(accountId, request);

            // Assert
            verify(coinWalletService, never()).debitCoin(any(), any(), any(), any(), any());
            verify(coinWalletService, never()).creditCoin(any(), any(), any(), any(), any());
        }

        @Test
        @DisplayName("Combo_001_09: Item type được normalize trước xử lý")
        void testCreateContentOrderNormalizesItemType() {
            // Arrange
            request.setItemType("combo"); // lowercase

            BigDecimal comboPrice = BigDecimal.valueOf(200.00);
            ContentOrderPreparationServiceImpl.ContentPriceResolution priceResolution =
                    new ContentOrderPreparationServiceImpl.ContentPriceResolution(
                            comboPrice, comboPrice, 0, 10);

            when(accountRepository.findById(accountId)).thenReturn(Optional.of(testAccount));
            when(contentOrderPreparationService.normalizeItemType("combo")).thenReturn("COMBO");
            when(contentOrderPreparationService.resolvePrice(accountId, "COMBO", itemId))
                    .thenReturn(priceResolution);
            when(orderRepository.findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
                    accountId, "COMBO", itemId, OrderStatus.AWAITING_PAYMENT))
                    .thenReturn(Optional.empty());
            when(orderRepository.nextPaymentCodeSequence()).thenReturn(1L);
            when(taxConfigService.getTaxConfigEntity()).thenReturn(testTaxConfig);
            when(sePayProperties.getOrderExpiryMinutes()).thenReturn(30);

            Order savedOrder = Order.builder()
                    .orderId("ord-normalized")
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId(itemId)
                    .totalAmount(comboPrice)
                    .fiatAmount(comboPrice)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .build();

            when(orderRepository.save(any(Order.class))).thenReturn(savedOrder);
            when(sePayService.buildQrUrl(any(), any())).thenReturn("qr-url");

            // Act
            OrderResponseDto response = orderService.createContentOrder(accountId, request);

            // Assert
            assertThat(response.getOrderId()).isEqualTo("ord-normalized");
            verify(contentOrderPreparationService, times(1)).normalizeItemType("combo");
        }
    }

    // ==================== ConfirmCoinPayment Tests ====================

    @Nested
    @DisplayName("confirmCoinPayment() - Coin Payment Confirmation Tests")
    class ConfirmCoinPaymentTests {

        private String orderId;

        @BeforeEach
        void setUp() {
            orderId = "ord-coin-confirm";
        }

        @Test
        @DisplayName("Combo_003_01: Hoàn tất order khi fiatAmount = 0 (Coin đủ)")
        void testConfirmCoinPaymentSuccess() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId("combo-001")
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .fiatAmount(BigDecimal.ZERO)
                    .coinAmount(200L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000001")
                    .build();

            when(orderRepository.findWithLockByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act
            OrderResponseDto response = orderService.confirmCoinPayment(orderId, accountId);

            // Assert
            assertThat(response).isNotNull();
            assertThat(response.getOrderId()).isEqualTo(orderId);

            verify(orderCompletionService, times(1))
                    .complete(order, BigDecimal.valueOf(100.00), PaymentMethod.COIN);
        }

        @Test
        @DisplayName("Combo_003_02: Ném ORDER_NOT_FOUND khi order không tồn tại")
        void testConfirmCoinPaymentThrowsWhenNotFound() {
            // Arrange
            when(orderRepository.findWithLockByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.empty());

            // Act & Assert
            assertThatThrownBy(() -> orderService.confirmCoinPayment(orderId, accountId))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_NOT_FOUND);

            verify(orderCompletionService, never()).complete(any(), any(), any());
        }

        @Test
        @DisplayName("Combo_003_03: Ném ORDER_NOT_CANCELLABLE khi status != AWAITING_PAYMENT")
        void testConfirmCoinPaymentThrowsWhenNotAwaitingPayment() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.COMPLETED)
                    .fiatAmount(BigDecimal.ZERO)
                    .build();

            when(orderRepository.findWithLockByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.confirmCoinPayment(orderId, accountId))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_NOT_CANCELLABLE);

            verify(orderCompletionService, never()).complete(any(), any(), any());
        }

        @Test
        @DisplayName("Combo_003_04: Ném ORDER_NOT_FULLY_COVERED_BY_COIN khi fiatAmount > 0")
        void testConfirmCoinPaymentThrowsWhenFiatRemaining() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .fiatAmount(BigDecimal.valueOf(50.00))
                    .coinAmount(50L)
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .build();

            when(orderRepository.findWithLockByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.confirmCoinPayment(orderId, accountId))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_NOT_FULLY_COVERED_BY_COIN);

            verify(orderCompletionService, never()).complete(any(), any(), any());
        }

        @Test
        @DisplayName("Combo_003_05: Ném ORDER_NOT_FULLY_COVERED_BY_COIN khi fiatAmount = null")
        void testConfirmCoinPaymentThrowsWhenFiatNull() {
            // Arrange
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .fiatAmount(null)
                    .coinAmount(100L)
                    .totalAmount(BigDecimal.valueOf(100.00))
                    .build();

            when(orderRepository.findWithLockByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            // Act & Assert
            assertThatThrownBy(() -> orderService.confirmCoinPayment(orderId, accountId))
                    .isInstanceOf(PaymentException.class)
                    .extracting("errorCode").isEqualTo(PaymentErrorCode.ORDER_NOT_FULLY_COVERED_BY_COIN);

            verify(orderCompletionService, never()).complete(any(), any(), any());
        }

        @Test
        @DisplayName("Combo_003_06: Gửi đúng totalAmount cho OrderCompletionService")
        void testConfirmCoinPaymentPassesCorrectAmount() {
            // Arrange
            BigDecimal totalAmount = BigDecimal.valueOf(250.50);
            Order order = Order.builder()
                    .orderId(orderId)
                    .account(testAccount)
                    .itemType("COMBO")
                    .itemId("combo-001")
                    .totalAmount(totalAmount)
                    .fiatAmount(BigDecimal.ZERO)
                    .coinAmount(501L)
                    .status(OrderStatus.AWAITING_PAYMENT)
                    .paymentCode("TLX000001")
                    .build();

            when(orderRepository.findWithLockByOrderIdAndAccountId(orderId, accountId))
                    .thenReturn(Optional.of(order));

            ArgumentCaptor<BigDecimal> amountCaptor = ArgumentCaptor.forClass(BigDecimal.class);
            ArgumentCaptor<PaymentMethod> methodCaptor = ArgumentCaptor.forClass(PaymentMethod.class);

            // Act
            orderService.confirmCoinPayment(orderId, accountId);

            // Assert
            verify(orderCompletionService, times(1))
                    .complete(eq(order), amountCaptor.capture(), methodCaptor.capture());

            assertThat(amountCaptor.getValue()).isEqualTo(totalAmount);
            assertThat(methodCaptor.getValue()).isEqualTo(PaymentMethod.COIN);
        }
    }
}
