package com.talex.server.services.impls;

import com.talex.server.exceptions.codes.AuthErrorCode;
import com.talex.server.exceptions.details.AuthException;
import com.talex.server.services.TokenFamilyService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class TokenFamilyServiceImpl implements TokenFamilyService {

    private final RedisTemplate<String, String> redisTemplate;

    private static final String FAMILY_PREFIX = "token_family:";
    private static final String ACCOUNT_INDEX_PREFIX = "account_families:";
    private static final String FIELD_ACCOUNT_ID = "accountId";
    private static final String FIELD_ACTIVE_TOKEN = "activeToken";

    @Value("${jwt.refresh-token-expiration}")
    private long refreshTokenExpirationMs;

    @Override
    public String createFamily(UUID accountId) {
        String familyId = UUID.randomUUID().toString();
        String tokenSecret = UUID.randomUUID().toString();
        String refreshToken = familyId + "." + tokenSecret;

        String familyKey = FAMILY_PREFIX + familyId;
        long ttlSeconds = refreshTokenExpirationMs / 1000;

        // Store token family as Redis HASH
        redisTemplate.opsForHash().putAll(familyKey, Map.of(
                FIELD_ACCOUNT_ID, accountId.toString(),
                FIELD_ACTIVE_TOKEN, refreshToken
        ));
        redisTemplate.expire(familyKey, ttlSeconds, TimeUnit.SECONDS);

        // Index: track which families belong to this account (for logout-all)
        // No TTL on index — individual family keys expire via their own TTL,
        // stale entries are cleaned lazily during deleteAllFamilies or deleteFamily
        String indexKey = ACCOUNT_INDEX_PREFIX + accountId;
        redisTemplate.opsForSet().add(indexKey, familyId);

        log.info("Token family created for account: {}", accountId);
        return refreshToken;
    }

    @Override
    public String validateAndRotate(String refreshToken) {
        String familyId = extractFamilyId(refreshToken);
        String familyKey = FAMILY_PREFIX + familyId;

        Map<Object, Object> familyData = redisTemplate.opsForHash().entries(familyKey);

        // Family not found → session expired
        if (familyData.isEmpty()) {
            log.warn("Token family not found (expired): {}", familyId);
            throw new AuthException(AuthErrorCode.SESSION_EXPIRED);
        }

        String activeToken = (String) familyData.get(FIELD_ACTIVE_TOKEN);
        String accountId = (String) familyData.get(FIELD_ACCOUNT_ID);

        // Token matches → legitimate refresh
        if (refreshToken.equals(activeToken)) {
            String newSecret = UUID.randomUUID().toString();
            String newRefreshToken = familyId + "." + newSecret;
            long ttlSeconds = refreshTokenExpirationMs / 1000;

            // Rotate: overwrite activeToken and renew TTL
            redisTemplate.opsForHash().put(familyKey, FIELD_ACTIVE_TOKEN, newRefreshToken);
            redisTemplate.expire(familyKey, ttlSeconds, TimeUnit.SECONDS);

            log.info("Token rotated for account: {}", accountId);
            return newRefreshToken;
        }

        // Token does NOT match → reuse detected (possible theft)
        log.warn("Token reuse detected! Destroying family: {} for account: {}", familyId, accountId);
        redisTemplate.delete(familyKey);
        cleanFamilyFromIndex(UUID.fromString(accountId), familyId);

        throw new AuthException(AuthErrorCode.TOKEN_REUSE_DETECTED);
    }

    @Override
    public UUID extractAccountId(String refreshToken) {
        String familyId = extractFamilyId(refreshToken);
        String familyKey = FAMILY_PREFIX + familyId;

        String accountId = (String) redisTemplate.opsForHash().get(familyKey, FIELD_ACCOUNT_ID);
        if (accountId == null) {
            throw new AuthException(AuthErrorCode.SESSION_EXPIRED);
        }

        return UUID.fromString(accountId);
    }

    @Override
    public void deleteFamily(String refreshToken) {
        String familyId = extractFamilyId(refreshToken);
        String familyKey = FAMILY_PREFIX + familyId;

        // Get accountId before deleting so we can clean the index
        String accountId = (String) redisTemplate.opsForHash().get(familyKey, FIELD_ACCOUNT_ID);

        redisTemplate.delete(familyKey);

        if (accountId != null) {
            cleanFamilyFromIndex(UUID.fromString(accountId), familyId);
        }

        log.info("Token family deleted: {}", familyId);
    }

    @Override
    public void deleteAllFamilies(UUID accountId) {
        String indexKey = ACCOUNT_INDEX_PREFIX + accountId;
        Set<String> familyIds = redisTemplate.opsForSet().members(indexKey);

        if (familyIds != null && !familyIds.isEmpty()) {
            // Batch delete all family keys at once
            List<String> keysToDelete = familyIds.stream()
                    .map(id -> FAMILY_PREFIX + id)
                    .toList();
            redisTemplate.delete(keysToDelete);
        }

        redisTemplate.delete(indexKey);
        log.info("All token families deleted for account: {}", accountId);
    }

    private String extractFamilyId(String refreshToken) {
        if (refreshToken == null || !refreshToken.contains(".")) {
            throw new AuthException(AuthErrorCode.SESSION_EXPIRED,
                    "Invalid refresh token format");
        }
        return refreshToken.substring(0, refreshToken.indexOf('.'));
    }

    private void cleanFamilyFromIndex(UUID accountId, String familyId) {
        String indexKey = ACCOUNT_INDEX_PREFIX + accountId;
        redisTemplate.opsForSet().remove(indexKey, familyId);
    }
}
