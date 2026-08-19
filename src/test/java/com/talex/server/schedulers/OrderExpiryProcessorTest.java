package com.talex.server.schedulers;

import com.talex.server.entities.auth.Account;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.coin.CoinReferenceType;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.coin.CoinWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("OrderExpiryProcessor Tests")
class OrderExpiryProcessorTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private CoinWalletService coinWalletService;

    @InjectMocks
    private OrderExpiryProcessor processor;

    private String testOrderId;
    private Account testAccount;
    private Order testOrder;
    private UUID testAccountId;

    @BeforeEach
    void setUp() {
        testOrderId = "order-001";
        testAccountId = UUID.randomUUID();

        testAccount = Account.builder()
                .accountId(testAccountId)
                .build();

        testOrder = Order.builder()
                .orderId(testOrderId)
                .status(OrderStatus.AWAITING_PAYMENT)
                .account(testAccount)
                .coinAmount(500L)
                .totalAmount(BigDecimal.valueOf(100000))
                .fiatAmount(BigDecimal.valueOf(100000))
                .build();
    }

    @Test
    @DisplayName("UTCID01: Có order hợp lệ, status AWAITING_PAYMENT, coinAmount > 0 → đánh dấu OUT_OF_TIME và hoàn Coin")
    void testOrderExpireWithCoinRefund() {
        // Arrange
        when(orderRepository.findWithLockByOrderId(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        processor.expireIfStillAwaiting(testOrderId);

        // Assert
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.OUT_OF_TIME);
        verify(orderRepository, times(1)).findWithLockByOrderId(testOrderId);
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getOrderId().equals(testOrderId) &&
                order.getStatus() == OrderStatus.OUT_OF_TIME
        ));
        verify(coinWalletService, times(1)).creditCoin(
                eq(testAccountId),
                eq(BigDecimal.valueOf(500L)),
                eq(CoinReferenceType.ORDER),
                eq(testOrderId),
                contains("hết hạn")
        );
    }

    @Test
    @DisplayName("UTCID02: Có order hợp lệ, status AWAITING_PAYMENT, coinAmount = 0 → đánh dấu OUT_OF_TIME, không hoàn Coin")
    void testOrderExpireWithZeroCoin() {
        // Arrange
        testOrder.setCoinAmount(0L);
        when(orderRepository.findWithLockByOrderId(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        processor.expireIfStillAwaiting(testOrderId);

        // Assert
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.OUT_OF_TIME);
        verify(orderRepository, times(1)).findWithLockByOrderId(testOrderId);
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getOrderId().equals(testOrderId) &&
                order.getStatus() == OrderStatus.OUT_OF_TIME
        ));
        verify(coinWalletService, never()).creditCoin(any(UUID.class), any(BigDecimal.class), any(), any(), any());
    }

    @Test
    @DisplayName("UTCID03: Order không tồn tại → skip (no-op)")
    void testOrderNotFound() {
        // Arrange
        when(orderRepository.findWithLockByOrderId(testOrderId)).thenReturn(Optional.empty());

        // Act
        processor.expireIfStillAwaiting(testOrderId);

        // Assert
        verify(orderRepository, times(1)).findWithLockByOrderId(testOrderId);
        verify(orderRepository, never()).save(any());
        verify(coinWalletService, never()).creditCoin(any(UUID.class), any(BigDecimal.class), any(), any(), any());
    }

    @Test
    @DisplayName("UTCID04: Order đã COMPLETED (xử lý concurrent webhook) → skip (no-op)")
    void testOrderAlreadyCompleted() {
        // Arrange
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findWithLockByOrderId(testOrderId)).thenReturn(Optional.of(testOrder));

        // Act
        processor.expireIfStillAwaiting(testOrderId);

        // Assert
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(orderRepository, times(1)).findWithLockByOrderId(testOrderId);
        verify(orderRepository, never()).save(any());
        verify(coinWalletService, never()).creditCoin(any(UUID.class), any(BigDecimal.class), any(), any(), any());
    }

    @Test
    @DisplayName("UTCID05: Order status CANCELLED (không phải AWAITING_PAYMENT) → skip (no-op)")
    void testOrderCancelled() {
        // Arrange
        testOrder.setStatus(OrderStatus.CANCELLED);
        when(orderRepository.findWithLockByOrderId(testOrderId)).thenReturn(Optional.of(testOrder));

        // Act
        processor.expireIfStillAwaiting(testOrderId);

        // Assert
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(orderRepository, times(1)).findWithLockByOrderId(testOrderId);
        verify(orderRepository, never()).save(any());
        verify(coinWalletService, never()).creditCoin(any(UUID.class), any(BigDecimal.class), any(), any(), any());
    }

    @Test
    @DisplayName("UTCID06: Order hợp lệ, status AWAITING_PAYMENT, coinAmount = null (boundary) → đánh dấu OUT_OF_TIME, không hoàn Coin")
    void testOrderExpireWithNullCoin() {
        // Arrange
        testOrder.setCoinAmount(null);
        when(orderRepository.findWithLockByOrderId(testOrderId)).thenReturn(Optional.of(testOrder));
        when(orderRepository.save(any(Order.class))).thenReturn(testOrder);

        // Act
        processor.expireIfStillAwaiting(testOrderId);

        // Assert
        assertThat(testOrder.getStatus()).isEqualTo(OrderStatus.OUT_OF_TIME);
        verify(orderRepository, times(1)).findWithLockByOrderId(testOrderId);
        verify(orderRepository, times(1)).save(argThat(order ->
                order.getOrderId().equals(testOrderId) &&
                order.getStatus() == OrderStatus.OUT_OF_TIME
        ));
        verify(coinWalletService, never()).creditCoin(any(UUID.class), any(BigDecimal.class), any(), any(), any());
    }
}
