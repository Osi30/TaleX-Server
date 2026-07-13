package com.talex.server.services.payment.impls;

import com.talex.server.configs.properties.SePayProperties;
import com.talex.server.dtos.requests.payment.SePayWebhookPayloadDto;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.enums.transaction.PaymentMethod;
import com.talex.server.repositories.transaction.OrderRepository;
import com.talex.server.services.payment.ISePayService;
import com.talex.server.services.payment.OrderCompletionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Slf4j
public class SePayService implements ISePayService {

    private static final String API_KEY_PREFIX = "Apikey ";
    private static final Pattern PAYMENT_CODE_PATTERN = Pattern.compile("(?i)TLX\\d{6}");

    private final SePayProperties sePayProperties;
    private final OrderRepository orderRepository;
    private final OrderCompletionService orderCompletionService;

    @Override
    public String buildQrUrl(String paymentCode, BigDecimal amount) {
        String prefix = sePayProperties.getTransferContentPrefix();
        String transferContent = prefix.isBlank() ? paymentCode : prefix + " " + paymentCode;
        return UriComponentsBuilder.fromUriString(sePayProperties.getQrBaseUrl())
                .queryParam("acc", sePayProperties.getAccountNumber())
                .queryParam("bank", sePayProperties.getBankName())
                .queryParam("amount", amount.toBigInteger())
                .queryParam("des", transferContent)
                .build()
                .toUriString();
    }

    @Override
    public boolean verifyApiKey(String authorizationHeader) {
        String expectedKey = sePayProperties.getWebhookApiKey();
        if (expectedKey == null || expectedKey.isBlank()) {
            log.warn("SEPAY_WEBHOOK_API_KEY is not configured — rejecting webhook");
            return false;
        }
        if (authorizationHeader == null || !authorizationHeader.startsWith(API_KEY_PREFIX)) {
            return false;
        }

        String providedKey = authorizationHeader.substring(API_KEY_PREFIX.length()).trim();
        return MessageDigest.isEqual(
                expectedKey.getBytes(StandardCharsets.UTF_8),
                providedKey.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    @Transactional
    public void handleWebhook(SePayWebhookPayloadDto payload) {
        if (!"in".equalsIgnoreCase(payload.getTransferType())) {
            log.info("Ignoring SePay webhook with transferType={}", payload.getTransferType());
            return;
        }

        String paymentCode = extractPaymentCode(payload.getContent());
        if (paymentCode == null) {
            log.warn("SePay webhook: no paymentCode found in content='{}'", payload.getContent());
            return;
        }

        Optional<Order> orderOpt = orderRepository.findWithLockByPaymentCode(paymentCode);
        if (orderOpt.isEmpty()) {
            log.warn("SePay webhook: no Order found for paymentCode={}", paymentCode);
            return;
        }

        Order order = orderOpt.get();
        if (order.getStatus() == OrderStatus.COMPLETED) {
            log.info("SePay webhook: order {} already COMPLETED, ignoring duplicate delivery", order.getOrderId());
            return;
        }
        if (order.getStatus() != OrderStatus.AWAITING_PAYMENT) {
            log.warn("SePay webhook: order {} has status={}, not creditable", order.getOrderId(), order.getStatus());
            return;
        }
        if (!sePayProperties.getAccountNumber().equals(payload.getAccountNumber())) {
            log.warn("SePay webhook: accountNumber mismatch for order {} (expected={}, got={})",
                    order.getOrderId(), sePayProperties.getAccountNumber(), payload.getAccountNumber());
            return;
        }
        BigDecimal amountDue = order.getFiatAmount() != null ? order.getFiatAmount() : order.getTotalAmount();
        if (payload.getTransferAmount() == null
                || payload.getTransferAmount().compareTo(amountDue) < 0) {
            log.warn("SePay webhook: amount short for order {} (expected>={}, got={})",
                    order.getOrderId(), amountDue, payload.getTransferAmount());
            return;
        }

        orderCompletionService.complete(order, payload.getTransferAmount(), PaymentMethod.SEPAY);
    }

    private String extractPaymentCode(String content) {
        if (content == null) {
            return null;
        }
        Matcher matcher = PAYMENT_CODE_PATTERN.matcher(content);
        return matcher.find() ? matcher.group().toUpperCase() : null;
    }
}
