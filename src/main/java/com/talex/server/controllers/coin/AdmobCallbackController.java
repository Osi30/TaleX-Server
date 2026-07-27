package com.talex.server.controllers.coin;

import com.talex.server.dtos.requests.coin.AdmobSsvCallbackRequest;
import com.talex.server.services.coin.AdmobVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
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
            @RequestParam MultiValueMap<String, String> queryParams,
            @RequestParam(name = "ad_network", required = false) String adNetwork,
            @RequestParam(name = "ad_unit", required = false) String adUnit,
            @RequestParam(name = "reward_amount", required = false) String rewardAmount,
            @RequestParam(name = "reward_item", required = false) String rewardItem,
            @RequestParam(name = "custom_data", required = false) String customData,
            @RequestParam(name = "signature", required = false) String signature,
            @RequestParam(name = "key_id", required = false) String keyId,
            @RequestParam(name = "transaction_id", required = false) String transactionId,
            @RequestParam(name = "timestamp", required = false) String timestamp,
            HttpServletRequest request) {

        log.info("[AdMob SSV] Received webhook request from Google. TransactionId: {}", transactionId);

        try {
            AdmobSsvCallbackRequest callback = new AdmobSsvCallbackRequest(
                    adNetwork,
                    adUnit,
                    rewardAmount,
                    rewardItem,
                    customData,
                    signature,
                    keyId,
                    transactionId,
                    timestamp,
                    request.getQueryString(),
                    queryParams);

            admobVerificationService.processAdmobReward(callback);

            return ResponseEntity.ok("OK");
        } catch (Exception exception) {
            log.error("[AdMob SSV] Failed to process webhook: {}", exception.getMessage());
            return ResponseEntity.badRequest().body("Verification Failed");
        }
    }
}
