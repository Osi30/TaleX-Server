package com.talex.server.services.coin.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.talex.server.dtos.requests.coin.AdmobCustomData;
import com.talex.server.dtos.responses.coin.AdmobKey;
import com.talex.server.dtos.responses.coin.AdmobKeyResponse;
import com.talex.server.services.coin.IMissionService;
import com.talex.server.services.coin.AdmobVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@RequiredArgsConstructor
public class AdmobVerificationServiceImpl implements AdmobVerificationService {

    private static final String GOOGLE_PUBLIC_KEYS_URL = "https://gstatic.com/admob/reward/verifier-keys.json";
    private static final String PUBLIC_KEYS_CACHE_KEY = "GOOGLE_ADMOB_PUBLIC_KEYS";
    private static final String TRANSACTION_KEY_PREFIX = "admob:txn:";

    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final IMissionService missionService;
    private final ObjectMapper objectMapper;

    private final Cache<String, AdmobKeyResponse> publicKeysCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    @Override
    public void processAdmobReward(
            String signature,
            String keyId,
            String customData,
            String transactionId,
            String timestamp,
            String queryString) {

        validateRequired(signature, "signature");
        validateRequired(keyId, "key_id");
        validateRequired(transactionId, "transaction_id");
        verifySignature(signature, keyId, queryString);

        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(
                TRANSACTION_KEY_PREFIX + transactionId,
                "COMPLETED",
                Duration.ofDays(7));

        if (!Boolean.TRUE.equals(isNew)) {
            log.info("[AdMob SSV] Duplicate transaction ignored. TransactionId: {}", transactionId);
            return;
        }

        AdmobCustomData rewardData = parseCustomData(customData);
        UUID accountId = UUID.fromString(rewardData.getAccountId());
        missionService.addProgress(accountId, rewardData.getMissionCode(), 1);

        log.info("[AdMob SSV] Reward processed. accountId={}, missionCode={}, transactionId={}, timestamp={}",
                accountId, rewardData.getMissionCode(), transactionId, timestamp);
    }

    private AdmobKeyResponse fetchGooglePublicKeys() {
        return publicKeysCache.get(PUBLIC_KEYS_CACHE_KEY, key -> {
            AdmobKeyResponse response = restTemplate.getForObject(GOOGLE_PUBLIC_KEYS_URL, AdmobKeyResponse.class);
            if (response == null || response.getKeys() == null || response.getKeys().isEmpty()) {
                throw new SecurityException("Unable to fetch AdMob public keys");
            }
            return response;
        });
    }

    private PublicKey getPublicKey(String keyId) {
        AdmobKey admobKey = fetchGooglePublicKeys().getKeys().stream()
                .filter(key -> keyId.equals(key.getKeyId()))
                .findFirst()
                .orElseThrow(() -> new SecurityException("AdMob public key not found"));

        try {
            byte[] keyBytes = Base64.getDecoder().decode(admobKey.getBase64());
            X509EncodedKeySpec keySpec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("EC").generatePublic(keySpec);
        } catch (Exception exception) {
            throw new SecurityException("Unable to create AdMob public key", exception);
        }
    }

    private void verifySignature(String signature, String keyId, String queryString) {
        try {
            String signedPayload = extractSignedPayload(queryString);
            PublicKey publicKey = getPublicKey(keyId);

            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(signedPayload.getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = decodeUrlSafeBase64(signature);
            if (!ecdsaVerify.verify(signatureBytes)) {
                throw new SecurityException("Invalid AdMob signature");
            }
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecurityException("Unable to verify AdMob signature", exception);
        }
    }

    private String extractSignedPayload(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            throw new SecurityException("Missing AdMob query string");
        }

        int signatureIndex = queryString.indexOf("&signature=");
        if (signatureIndex >= 0) {
            return queryString.substring(0, signatureIndex);
        }

        if (queryString.startsWith("signature=")) {
            throw new SecurityException("Missing AdMob signed payload");
        }

        throw new SecurityException("Missing AdMob signature parameter");
    }

    private byte[] decodeUrlSafeBase64(String value) {
        int padding = (4 - value.length() % 4) % 4;
        return Base64.getUrlDecoder().decode(value + "=".repeat(padding));
    }

    private AdmobCustomData parseCustomData(String customData) {
        if (customData == null || customData.isBlank()) {
            throw new IllegalArgumentException("Missing AdMob custom_data");
        }

        try {
            AdmobCustomData rewardData = objectMapper.readValue(customData, AdmobCustomData.class);
            if (rewardData.getAccountId() == null || rewardData.getAccountId().isBlank()
                    || rewardData.getMissionCode() == null || rewardData.getMissionCode().isBlank()) {
                throw new IllegalArgumentException("Invalid AdMob custom_data");
            }
            return rewardData;
        } catch (IllegalArgumentException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new IllegalArgumentException("Unable to parse AdMob custom_data", exception);
        }
    }

    private void validateRequired(String value, String fieldName) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing AdMob " + fieldName);
        }
    }
}
