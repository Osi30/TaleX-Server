package com.talex.server.services.payment.impls;

import com.talex.server.dtos.requests.payment.OrderInterventionRequestDto;
import com.talex.server.entities.auth.Account;
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
import com.talex.server.services.payment.OrderCompletionService;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AdminOrderInterventionServiceImpl Tests")
class AdminOrderInterventionServiceImplTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderInterventionLogRepository orderInterventionLogRepository;

    @Mock
    private CoinWalletService coinWalletService;

    @Mock
    private CampaignWalletService campaignWalletService;

    @Mock
    private OrderCompletionService orderCompletionService;

    @InjectMocks
    private AdminOrderInterventionServiceImpl service;

    private String orderId;
    private UUID adminId;
    private UUID accountId;
    private Account account;
    private String reason;

    @BeforeEach
    void setUp() {
        orderId = "order-001";
        adminId = UUID.randomUUID();
        accountId = UUID.randomUUID();
        account = Account.builder().accountId(accountId).build();
        reason = "Đã đối chiếu sao kê ngân hàng, xác nhận tiền đã về";
    }

    private Order newOrder(OrderStatus status, BigDecimal totalAmount, BigDecimal fiatAmount,
                            Long coinAmount, BigDecimal campaignWalletAmount) {
        return Order.builder()
                .orderId(orderId)
                .paymentCode("TLX000001")
                .status(status)
                .account(account)
                .itemType("COMBO")
                .totalAmount(totalAmount)
                .fiatAmount(fiatAmount)
                .coinAmount(coinAmount)
                .campaignWalletAmount(campaignWalletAmount)
                .build();
    }

    // ── forceCompleteByAdmin ──────────────────────────────────────

    @Test
    @DisplayName("UTCID01: forceComplete đơn AWAITING_PAYMENT còn fiatAmount > 0 → complete() gọi đúng 1 lần với fiatAmount, ADMIN_MANUAL, log FORCE_COMPLETE ghi trước khi complete")
    void testForceCompleteAwaitingPaymentWithFiat() {
        Order order = newOrder(OrderStatus.AWAITING_PAYMENT, BigDecimal.valueOf(100000),
                BigDecimal.valueOf(60000), 400L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        Order result = service.forceCompleteByAdmin(orderId, adminId, reason);

        assertThat(result).isSameAs(order);
        verify(orderCompletionService, times(1))
                .complete(order, BigDecimal.valueOf(60000), PaymentMethod.ADMIN_MANUAL);
        verify(orderCompletionService, never()).completeViaWalletOnly(any());

        ArgumentCaptor<OrderInterventionLog> logCaptor = ArgumentCaptor.forClass(OrderInterventionLog.class);
        verify(orderInterventionLogRepository, times(1)).save(logCaptor.capture());
        OrderInterventionLog savedLog = logCaptor.getValue();
        assertThat(savedLog.getOrderId()).isEqualTo(orderId);
        assertThat(savedLog.getPaymentCode()).isEqualTo("TLX000001");
        assertThat(savedLog.getAction()).isEqualTo(OrderInterventionAction.FORCE_COMPLETE);
        assertThat(savedLog.getPreviousStatus()).isEqualTo("AWAITING_PAYMENT");
        assertThat(savedLog.getNewStatus()).isEqualTo("COMPLETED");
        assertThat(savedLog.getAdminAccountId()).isEqualTo(adminId.toString());
        assertThat(savedLog.getReason()).isEqualTo(reason);
    }

    @Test
    @DisplayName("UTCID02: forceComplete đơn đã COMPLETED → ném ORDER_NOT_INTERVENABLE, complete()/log KHÔNG được gọi (chặn double-fulfill)")
    void testForceCompleteAlreadyCompletedRejected() {
        Order order = newOrder(OrderStatus.COMPLETED, BigDecimal.valueOf(100000),
                BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.forceCompleteByAdmin(orderId, adminId, reason))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.ORDER_NOT_INTERVENABLE);

        verify(orderCompletionService, never()).complete(any(), any(), any());
        verify(orderCompletionService, never()).completeViaWalletOnly(any());
        verify(orderInterventionLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID03: forceComplete đơn CANCELLED → reject, không gọi complete/log")
    void testForceCompleteCancelledRejected() {
        Order order = newOrder(OrderStatus.CANCELLED, BigDecimal.valueOf(100000),
                BigDecimal.ZERO, 0L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.forceCompleteByAdmin(orderId, adminId, reason))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.ORDER_NOT_INTERVENABLE);

        verify(orderCompletionService, never()).complete(any(), any(), any());
        verify(orderCompletionService, never()).completeViaWalletOnly(any());
        verify(orderInterventionLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID04: forceComplete đơn OUT_OF_TIME → vẫn cho phép (tiền về trễ sau khi hệ thống tự đánh hết hạn)")
    void testForceCompleteOutOfTimeAllowed() {
        Order order = newOrder(OrderStatus.OUT_OF_TIME, BigDecimal.valueOf(50000),
                BigDecimal.valueOf(50000), 0L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        service.forceCompleteByAdmin(orderId, adminId, reason);

        verify(orderCompletionService, times(1))
                .complete(order, BigDecimal.valueOf(50000), PaymentMethod.ADMIN_MANUAL);
        verify(orderInterventionLogRepository, times(1)).save(any(OrderInterventionLog.class));
    }

    @Test
    @DisplayName("UTCID05: forceComplete đơn fiatAmount=0, trả 100% Campaign Wallet → completeViaWalletOnly() gọi, complete() KHÔNG gọi")
    void testForceCompleteWalletOnly() {
        Order order = newOrder(OrderStatus.AWAITING_PAYMENT, BigDecimal.valueOf(100000),
                BigDecimal.ZERO, 0L, BigDecimal.valueOf(100000));
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        service.forceCompleteByAdmin(orderId, adminId, reason);

        verify(orderCompletionService, times(1)).completeViaWalletOnly(order);
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID06: forceComplete đơn fiatAmount=0, trả 100% bằng Coin (không wallet) → complete() gọi với totalAmount, ADMIN_MANUAL")
    void testForceCompleteCoinOnlyUsesTotalAmount() {
        Order order = newOrder(OrderStatus.AWAITING_PAYMENT, BigDecimal.valueOf(80000),
                BigDecimal.ZERO, 800L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        service.forceCompleteByAdmin(orderId, adminId, reason);

        verify(orderCompletionService, times(1))
                .complete(order, BigDecimal.valueOf(80000), PaymentMethod.ADMIN_MANUAL);
        verify(orderCompletionService, never()).completeViaWalletOnly(any());
    }

    // ── cancelByAdmin ─────────────────────────────────────────────

    @Test
    @DisplayName("UTCID07: cancel đơn có coinAmount>0 → creditCoin gọi đúng số, status CANCELLED, log CANCEL ghi")
    void testCancelRefundsCoin() {
        Order order = newOrder(OrderStatus.AWAITING_PAYMENT, BigDecimal.valueOf(100000),
                BigDecimal.valueOf(60000), 400L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        Order result = service.cancelByAdmin(orderId, adminId, reason);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(coinWalletService, times(1)).creditCoin(
                eq(accountId), eq(BigDecimal.valueOf(400L)), eq(CoinReferenceType.ORDER),
                eq(orderId), anyString());
        verify(campaignWalletService, never()).creditWallet(any(), any(), any(), any());
        verify(orderRepository, times(1)).save(order);

        ArgumentCaptor<OrderInterventionLog> logCaptor = ArgumentCaptor.forClass(OrderInterventionLog.class);
        verify(orderInterventionLogRepository, times(1)).save(logCaptor.capture());
        assertThat(logCaptor.getValue().getAction()).isEqualTo(OrderInterventionAction.CANCEL);
        assertThat(logCaptor.getValue().getPreviousStatus()).isEqualTo("AWAITING_PAYMENT");
        assertThat(logCaptor.getValue().getNewStatus()).isEqualTo("CANCELLED");
    }

    @Test
    @DisplayName("UTCID08: cancel đơn có campaignWalletAmount>0 → creditWallet gọi đúng số")
    void testCancelRefundsCampaignWallet() {
        Order order = newOrder(OrderStatus.AWAITING_PAYMENT, BigDecimal.valueOf(100000),
                BigDecimal.ZERO, 0L, BigDecimal.valueOf(100000));
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        service.cancelByAdmin(orderId, adminId, reason);

        verify(campaignWalletService, times(1))
                .creditWallet(eq(accountId), eq(BigDecimal.valueOf(100000)), anyString(), eq(orderId));
        verify(coinWalletService, never()).creditCoin(any(), any(), any(), any(), any());
    }

    @Test
    @DisplayName("UTCID09: cancel đơn không Coin không Wallet → không gọi refund nào, vẫn CANCELLED + log")
    void testCancelNoRefundNeeded() {
        Order order = newOrder(OrderStatus.OUT_OF_TIME, BigDecimal.valueOf(50000),
                BigDecimal.valueOf(50000), 0L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        Order result = service.cancelByAdmin(orderId, adminId, reason);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CANCELLED);
        verify(coinWalletService, never()).creditCoin(any(), any(), any(), any(), any());
        verify(campaignWalletService, never()).creditWallet(any(), any(), any(), any());
        verify(orderInterventionLogRepository, times(1)).save(any(OrderInterventionLog.class));
    }

    @Test
    @DisplayName("UTCID10: cancel đơn COMPLETED → reject, không refund, không đổi status")
    void testCancelCompletedRejected() {
        Order order = newOrder(OrderStatus.COMPLETED, BigDecimal.valueOf(100000),
                BigDecimal.ZERO, 400L, BigDecimal.ZERO);
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> service.cancelByAdmin(orderId, adminId, reason))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.ORDER_NOT_INTERVENABLE);

        assertThat(order.getStatus()).isEqualTo(OrderStatus.COMPLETED);
        verify(coinWalletService, never()).creditCoin(any(), any(), any(), any(), any());
        verify(campaignWalletService, never()).creditWallet(any(), any(), any(), any());
        verify(orderRepository, never()).save(any());
        verify(orderInterventionLogRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID11: order không tồn tại → ném ORDER_NOT_FOUND cho cả cancel và forceComplete")
    void testOrderNotFoundThrows() {
        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.cancelByAdmin(orderId, adminId, reason))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.ORDER_NOT_FOUND);

        when(orderRepository.findWithLockByOrderId(orderId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.forceCompleteByAdmin(orderId, adminId, reason))
                .isInstanceOf(PaymentException.class)
                .extracting("errorCode")
                .isEqualTo(PaymentErrorCode.ORDER_NOT_FOUND);
    }

    // ── DTO validation (reason bắt buộc) ──────────────────────────
    // Bean Validation (@NotBlank) chỉ chạy qua @Valid ở tầng Controller, không phải ở service —
    // validate trực tiếp DTO bằng Validator độc lập, không cần Spring context/DB thật.

    @Test
    @DisplayName("UTCID12: OrderInterventionRequestDto reason blank → vi phạm @NotBlank")
    void testRequestDtoBlankReasonInvalid() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        OrderInterventionRequestDto dto = new OrderInterventionRequestDto();
        dto.setReason("   ");

        Set<ConstraintViolation<OrderInterventionRequestDto>> violations = validator.validate(dto);

        assertThat(violations).isNotEmpty();
        assertThat(violations).anyMatch(v -> v.getPropertyPath().toString().equals("reason"));
    }

    @Test
    @DisplayName("UTCID13: OrderInterventionRequestDto reason hợp lệ → không vi phạm")
    void testRequestDtoValidReason() {
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        OrderInterventionRequestDto dto = new OrderInterventionRequestDto();
        dto.setReason(reason);

        Set<ConstraintViolation<OrderInterventionRequestDto>> violations = validator.validate(dto);

        assertThat(violations).isEmpty();
    }
}
