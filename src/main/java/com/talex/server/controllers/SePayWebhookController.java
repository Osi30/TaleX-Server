package com.talex.server.controllers;

import com.talex.server.dtos.requests.payment.SePayWebhookPayloadDto;
import com.talex.server.services.payment.ISePayService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Payments", description = "Webhook nhận thông báo giao dịch từ SePay")
public class SePayWebhookController {

    private final ISePayService sePayService;

    @PostMapping("/api/v1/payments/sepay-webhook")
    @Operation(summary = "SePay webhook", description = "SePay gọi vào endpoint này khi có biến động số dư tài khoản ngân hàng.")
    public ResponseEntity<Map<String, Boolean>> handleSePayWebhook(
            @RequestBody SePayWebhookPayloadDto payload,
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        if (!sePayService.verifyApiKey(authorization)) {
            log.warn("SePay webhook rejected: invalid or missing API key");
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(Map.of("success", false));
        }

        sePayService.handleWebhook(payload);
        return ResponseEntity.ok(Map.of("success", true));
    }
}
