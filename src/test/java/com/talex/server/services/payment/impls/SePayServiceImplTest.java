package com.talex.server.services.payment.impls;

import com.talex.server.configs.properties.SePayProperties;
import com.talex.server.dtos.requests.payment.SePayWebhookPayloadDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.payment.OrderCompletionService;
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

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SePayServiceImpl.handleWebhook Tests")
class SePayServiceImplTest {

    @Mock
    private SePayProperties sePayProperties;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OrderCompletionService orderCompletionService;

    @InjectMocks
    private SePayServiceImpl sePayService;

    private static final String TEST_ACCOUNT_NUMBER = "100881945065";
    private static final String TEST_WEBHOOK_API_KEY = "j7gofpsEkVGQN0TBFrltyIO926ScMAC1DPqxZaKH";
    private static final String TEST_PAYMENT_CODE = "TLX100234";

    private Order testOrder;
    private SePayWebhookPayloadDto testPayload;

    @BeforeEach
    void setUp() {
        // Setup SePayProperties defaults — lenient vì không phải test nào cũng chạm
        // tới cả 2 stub này (VD test dừng sớm ở bước validate payload trước khi tới
        // bước so khớp account number/api key), Mockito strict-stub sẽ báo
        // UnnecessaryStubbing nếu dùng when() thường cho case đó.
        lenient().when(sePayProperties.getAccountNumber()).thenReturn(TEST_ACCOUNT_NUMBER);
        lenient().when(sePayProperties.getWebhookApiKey()).thenReturn(TEST_WEBHOOK_API_KEY);

        // Create test order
        testOrder = Order.builder()
                .orderId("order-123")
                .paymentCode(TEST_PAYMENT_CODE)
                .totalAmount(new BigDecimal("100000"))
                .fiatAmount(new BigDecimal("100000"))
                .coinAmount(0L)
                .status(OrderStatus.AWAITING_PAYMENT)
                .build();

        // Create test payload
        testPayload = new SePayWebhookPayloadDto();
        testPayload.setId(92704L);
        testPayload.setGateway("VietinBank");
        testPayload.setTransactionDate("2024-07-02 11:08:33");
        testPayload.setAccountNumber(TEST_ACCOUNT_NUMBER);
        testPayload.setSubAccount("");
        testPayload.setCode("SEVN63DC8E5C");
        testPayload.setContent("TLX100234 chuyen tien");
        testPayload.setTransferType("in");
        testPayload.setDescription("NGUYEN VAN A chuyen tien");
        testPayload.setTransferAmount(new BigDecimal("100000"));
        testPayload.setAccumulated(new BigDecimal("105000000"));
        testPayload.setReferenceCode("FT24012345678");
    }

    @Test
    @DisplayName("UTCID01: Valid webhook payload với transferType='in', exact amount → complete order")
    void testHandleWebhook_ValidPayload_ExactAmount() {
        // Arrange
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(
                testOrder,
                new BigDecimal("100000"),
                PaymentMethod.SEPAY
        );
    }

    @Test
    @DisplayName("UTCID02: Valid webhook payload với transferType='in', overpay amount → complete with overpaid flag")
    void testHandleWebhook_ValidPayload_OverpayAmount() {
        // Arrange
        BigDecimal overpayAmount = new BigDecimal("150000");
        testPayload.setTransferAmount(overpayAmount);

        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        ArgumentCaptor<Order> orderCaptor = ArgumentCaptor.forClass(Order.class);
        verify(orderCompletionService, times(1)).complete(
                orderCaptor.capture(),
                eq(overpayAmount),
                eq(PaymentMethod.SEPAY)
        );

        // Verify overpaid_amount set correctly (expected = 150000 - 100000 = 50000)
        Order capturedOrder = orderCaptor.getValue();
        assertThat(capturedOrder.getOverpaidAmount()).isEqualTo(new BigDecimal("50000"));
    }

    @Test
    @DisplayName("UTCID03: transferType='out' (tiền ra, không phải tiền vào) → ignore webhook")
    void testHandleWebhook_NonIncomingTransferType() {
        // Arrange
        testPayload.setTransferType("out");

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderRepository, never()).findWithLockByPaymentCode(anyString());
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID04: Content không chứa paymentCode (pattern TLX\\d{6}) → ignore webhook")
    void testHandleWebhook_NoPaymentCodeInContent() {
        // Arrange
        testPayload.setContent("random content without payment code");

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderRepository, never()).findWithLockByPaymentCode(anyString());
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID05: Order không tồn tại (no matching paymentCode) → ignore webhook")
    void testHandleWebhook_OrderNotFound() {
        // Arrange
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.empty());

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID06: Order đã COMPLETED trước đó (duplicate webhook) → ignore webhook")
    void testHandleWebhook_OrderAlreadyCompleted() {
        // Arrange
        testOrder.setStatus(OrderStatus.COMPLETED);
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID07: Order status không phải AWAITING_PAYMENT (e.g., OUT_OF_TIME) → ignore webhook")
    void testHandleWebhook_OrderNotAwaitingPayment() {
        // Arrange
        testOrder.setStatus(OrderStatus.OUT_OF_TIME);
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID08: Account number mismatch (webhook STK ≠ config STK) → ignore webhook")
    void testHandleWebhook_AccountNumberMismatch() {
        // Arrange
        testPayload.setAccountNumber("999999999999"); // Wrong account number
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("UTCID09: Payment amount < due amount (amount short) → ignore webhook")
    void testHandleWebhook_PaymentAmountShort() {
        // Arrange
        testPayload.setTransferAmount(new BigDecimal("50000")); // Less than due 100000
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("transferType case-insensitive: 'IN' (uppercase) should be accepted")
    void testHandleWebhook_TransferTypeUppercase() {
        // Arrange
        testPayload.setTransferType("IN");
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(any(), any(), any());
    }

    @Test
    @DisplayName("Payment code case-insensitive: 'tlx100234' (lowercase) should match 'TLX100234'")
    void testHandleWebhook_PaymentCodeLowercase() {
        // Arrange
        testPayload.setContent("tlx100234 chuyen tien");
        when(orderRepository.findWithLockByPaymentCode("TLX100234"))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(any(), any(), any());
    }

    @Test
    @DisplayName("Use fiatAmount when available, fallback to totalAmount if fiatAmount is null")
    void testHandleWebhook_UseFiatAmountWhenAvailable() {
        // Arrange
        testOrder.setFiatAmount(new BigDecimal("95000")); // Use fiat instead of total
        testPayload.setTransferAmount(new BigDecimal("95000"));
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(
                testOrder,
                new BigDecimal("95000"),
                PaymentMethod.SEPAY
        );
    }

    @Test
    @DisplayName("Fallback to totalAmount when fiatAmount is null")
    void testHandleWebhook_FallbackToTotalAmountWhenFiatNull() {
        // Arrange
        testOrder.setFiatAmount(null);
        testOrder.setTotalAmount(new BigDecimal("100000"));
        testPayload.setTransferAmount(new BigDecimal("100000"));
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(
                testOrder,
                new BigDecimal("100000"),
                PaymentMethod.SEPAY
        );
    }

    @Test
    @DisplayName("Payment amount >= due amount (equal) should be accepted")
    void testHandleWebhook_ExactAmountMatch() {
        // Arrange
        testPayload.setTransferAmount(new BigDecimal("100000"));
        when(orderRepository.findWithLockByPaymentCode(TEST_PAYMENT_CODE))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(any(), any(), any());
    }

    @Test
    @DisplayName("Payment code extracted from middle of content: 'blah TLX999999 blah'")
    void testHandleWebhook_PaymentCodeExtractedFromMiddle() {
        // Arrange
        testPayload.setContent("blah blah TLX999999 blah");
        testOrder.setPaymentCode("TLX999999");
        when(orderRepository.findWithLockByPaymentCode("TLX999999"))
                .thenReturn(Optional.of(testOrder));

        // Act
        sePayService.handleWebhook(testPayload);

        // Assert
        verify(orderCompletionService, times(1)).complete(any(), any(), any());
    }

    @Test
    @DisplayName("Null content should not crash, simply log warning and return")
    void testHandleWebhook_NullContent() {
        // Arrange
        testPayload.setContent(null);

        // Act & Assert (should not throw exception)
        assertThatNoException().isThrownBy(() -> sePayService.handleWebhook(testPayload));

        // Verify webhook was ignored
        verify(orderRepository, never()).findWithLockByPaymentCode(anyString());
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }

    @Test
    @DisplayName("Null transferType should not crash, simply log and return")
    void testHandleWebhook_NullTransferType() {
        // Arrange
        testPayload.setTransferType(null);

        // Act & Assert (should not throw exception)
        assertThatNoException().isThrownBy(() -> sePayService.handleWebhook(testPayload));

        // Verify webhook was ignored
        verify(orderRepository, never()).findWithLockByPaymentCode(anyString());
        verify(orderCompletionService, never()).complete(any(), any(), any());
    }
}
