package com.talex.server.services.coin.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.talex.server.dtos.requests.coin.AdmobCustomData;
import com.talex.server.dtos.requests.coin.AdmobSsvCallbackRequest;
import com.talex.server.dtos.responses.coin.AdmobKey;
import com.talex.server.dtos.responses.coin.AdmobKeyResponse;
import com.talex.server.services.coin.IMissionService;
import com.talex.server.services.coin.AdmobVerificationService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLDecoder;
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

    private static final String PUBLIC_KEYS_CACHE_KEY = "GOOGLE_ADMOB_PUBLIC_KEYS";
    private static final String TRANSACTION_KEY_PREFIX = "admob:txn:";
    private static final Duration PROCESSING_TTL = Duration.ofMinutes(10);
    private static final Duration COMPLETED_TTL = Duration.ofDays(30);
    private static final String PROCESSING_STATUS = "PROCESSING";
    private static final String COMPLETED_STATUS = "COMPLETED";

    private final RestTemplate restTemplate;
    private final StringRedisTemplate stringRedisTemplate;
    private final IMissionService missionService;
    private final ObjectMapper objectMapper;

    @Value("${admob.ssv.public-keys-url:https://www.gstatic.com/admob/reward/verifier-keys.json}")
    private String publicKeysUrl;

    @Value("${admob.ssv.signature-verification-enabled:true}")
    private boolean signatureVerificationEnabled;

    private final Cache<String, AdmobKeyResponse> publicKeysCache = Caffeine.newBuilder()
            .expireAfterWrite(24, TimeUnit.HOURS)
            .maximumSize(1)
            .build();

    @Override
    public void processAdmobReward(AdmobSsvCallbackRequest callback) {
        validateCallback(callback);
        verifySignature(callback);

        String redisKey = TRANSACTION_KEY_PREFIX + callback.transactionId();
        Boolean isNew = stringRedisTemplate.opsForValue().setIfAbsent(
                redisKey,
                PROCESSING_STATUS,
                PROCESSING_TTL);

        if (!Boolean.TRUE.equals(isNew)) {
            log.info("[AdMob SSV] Duplicate transaction ignored. TransactionId: {}", callback.transactionId());
            return;
        }

        try {
            AdmobCustomData rewardData = parseCustomData(callback.customData());
            UUID accountId = UUID.fromString(rewardData.getAccountId());
            missionService.addProgress(accountId, rewardData.getMissionCode(), 1);

            log.info("[AdMob SSV] Reward processed. accountId={}, missionCode={}, transactionId={}, timestamp={}",
                    accountId, rewardData.getMissionCode(), callback.transactionId(), callback.timestamp());

            stringRedisTemplate.opsForValue().set(redisKey, COMPLETED_STATUS, COMPLETED_TTL);
        } catch (Exception exception) {
            stringRedisTemplate.delete(redisKey);
            throw exception;
        }
    }

    private AdmobKeyResponse fetchGooglePublicKeys() {
        return publicKeysCache.get(PUBLIC_KEYS_CACHE_KEY, key -> {
            AdmobKeyResponse response = restTemplate.getForObject(publicKeysUrl, AdmobKeyResponse.class);
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

    private void verifySignature(AdmobSsvCallbackRequest callback) {
        if (!signatureVerificationEnabled) {
            log.warn("[AdMob SSV] Signature verification is DISABLED. rawQuery={}, params={}",
                    callback.queryString(), callback.queryParams());
            return;
        }

        try {
            SignedPayload signedPayload = extractSignedPayload(callback.queryString());
            if (!signedPayload.signature().equals(callback.signature())
                    || !signedPayload.keyId().equals(callback.keyId())) {
                throw new SecurityException("AdMob signature/key_id mismatch");
            }

            PublicKey publicKey = getPublicKey(signedPayload.keyId());

            Signature ecdsaVerify = Signature.getInstance("SHA256withECDSA");
            ecdsaVerify.initVerify(publicKey);
            ecdsaVerify.update(signedPayload.payload().getBytes(StandardCharsets.UTF_8));

            byte[] signatureBytes = decodeUrlSafeBase64(signedPayload.signature());
            if (!ecdsaVerify.verify(signatureBytes)) {
                throw new SecurityException("Invalid AdMob signature");
            }
        } catch (SecurityException exception) {
            throw exception;
        } catch (Exception exception) {
            throw new SecurityException("Unable to verify AdMob signature", exception);
        }
    }

    private SignedPayload extractSignedPayload(String queryString) {
        if (queryString == null || queryString.isBlank()) {
            throw new SecurityException("Missing AdMob query string");
        }

        int signatureIndex = queryString.indexOf("&signature=");
        if (signatureIndex <= 0) {
            throw new SecurityException("AdMob signature and key_id must be the last two query parameters");
        }

        String signedPayload = queryString.substring(0, signatureIndex);
        String signatureAndKeyId = queryString.substring(signatureIndex + 1);

        int keyIdIndex = signatureAndKeyId.indexOf("&key_id=");
        if (keyIdIndex <= 0) {
            throw new SecurityException("AdMob signature and key_id must be the last two query parameters");
        }

        String signature = signatureAndKeyId.substring("signature=".length(), keyIdIndex);
        String keyId = signatureAndKeyId.substring(keyIdIndex + "&key_id=".length());
        try {
            Long.parseLong(keyId);
        } catch (NumberFormatException exception) {
            throw new SecurityException("AdMob key_id must be a long", exception);
        }

        return new SignedPayload(signedPayload, signature, keyId);
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
            String decodedCustomData = URLDecoder.decode(customData, StandardCharsets.UTF_8);
            AdmobCustomData rewardData = objectMapper.readValue(decodedCustomData, AdmobCustomData.class);
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

    private void validateCallback(AdmobSsvCallbackRequest callback) {
        validateRequired(callback.adNetwork(), "ad_network");
        validateRequired(callback.adUnit(), "ad_unit");
        validateRequired(callback.rewardAmount(), "reward_amount");
        validateRequired(callback.rewardItem(), "reward_item");
        validateRequired(callback.customData(), "custom_data");
        validateRequired(callback.signature(), "signature");
        validateRequired(callback.keyId(), "key_id");
        validateRequired(callback.transactionId(), "transaction_id");
        validateRequired(callback.timestamp(), "timestamp");
    }

    private record SignedPayload(String payload, String signature, String keyId) {
    }
}
