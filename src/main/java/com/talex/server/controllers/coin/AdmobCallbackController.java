package com.talex.server.controllers.coin;

import com.talex.server.services.coin.AdmobVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/callbacks")
@RequiredArgsConstructor
@Slf4j
public class AdmobCallbackController {

    private final AdmobVerificationService admobVerificationService;

    @GetMapping("/admob-reward")
    public ResponseEntity<String> handleAdmobSSVCallback(
            @RequestParam(name = "signature") String signature,
            @RequestParam(name = "key_id") String keyId,
            @RequestParam(name = "custom_data", required = false) String customData,
            @RequestParam(name = "transaction_id") String transactionId,
            @RequestParam(name = "timestamp") String timestamp,
            HttpServletRequest request) {

        log.info("[AdMob SSV] Received webhook request from Google. TransactionId: {}", transactionId);

        try {
            String queryString = request.getQueryString();

            admobVerificationService.processAdmobReward(
                    signature, keyId, customData, transactionId, timestamp, queryString);

            return ResponseEntity.ok("OK");
        } catch (Exception exception) {
            log.error("[AdMob SSV] Failed to process webhook: {}", exception.getMessage());
            return ResponseEntity.badRequest().body("Verification Failed");
        }
    }
}
