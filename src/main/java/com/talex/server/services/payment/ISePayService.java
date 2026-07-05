package com.talex.server.services.payment;

import com.talex.server.dtos.requests.payment.SePayWebhookPayloadDto;

import java.math.BigDecimal;

public interface ISePayService {
    String buildQrUrl(String paymentCode, BigDecimal amount);

    boolean verifyApiKey(String authorizationHeader);

    void handleWebhook(SePayWebhookPayloadDto payload);
}
