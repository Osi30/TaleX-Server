package com.talex.server.services.media.impls;

import com.talex.server.entities.media.MediaUploadSession;
import com.talex.server.enums.media.MediaUploadSessionStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
@Slf4j
public class MediaUploadProgressCache {
    private static final String KEY_PREFIX = "media-upload-session:progress:";
    private static final String FIELD_UPLOADED_BYTES = "uploadedBytes";
    private static final String FIELD_LAST_CHUNK_INDEX = "lastUploadedChunkIndex";
    private static final String FIELD_STATUS = "status";
    private static final String FIELD_ACTOR_ID = "actorId";
    private static final String FIELD_UPDATED_AT = "updatedAt";
    private static final Duration DEFAULT_TTL = Duration.ofHours(30);
    private static final Duration MIN_TTL = Duration.ofHours(1);
    private static final Duration EXPIRY_GRACE = Duration.ofHours(1);

    private final RedisTemplate<String, String> redisTemplate;

    Optional<CachedMediaUploadProgress> get(String uploadSessionId) {
        try {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key(uploadSessionId));
            if (entries == null || entries.isEmpty()) {
                return Optional.empty();
            }
            return Optional.of(new CachedMediaUploadProgress(
                    parseLong(entries.get(FIELD_UPLOADED_BYTES)),
                    parseInteger(entries.get(FIELD_LAST_CHUNK_INDEX)),
                    parseStatus(entries.get(FIELD_STATUS)),
                    stringValue(entries.get(FIELD_ACTOR_ID))));
        } catch (RuntimeException ex) {
            log.warn("Cannot read media upload progress from Redis. uploadSessionId={}", uploadSessionId, ex);
            return Optional.empty();
        }
    }

    boolean put(String uploadSessionId, CachedMediaUploadProgress progress, MediaUploadSession session) {
        try {
            Map<String, String> fields = new HashMap<>();
            fields.put(FIELD_UPLOADED_BYTES, String.valueOf(progress.uploadedBytes()));
            if (progress.lastUploadedChunkIndex() != null) {
                fields.put(FIELD_LAST_CHUNK_INDEX, String.valueOf(progress.lastUploadedChunkIndex()));
            }
            if (progress.status() != null) {
                fields.put(FIELD_STATUS, progress.status().name());
            }
            if (progress.actorId() != null && !progress.actorId().isBlank()) {
                fields.put(FIELD_ACTOR_ID, progress.actorId().trim());
            }
            fields.put(FIELD_UPDATED_AT, LocalDateTime.now().toString());

            String key = key(uploadSessionId);
            redisTemplate.opsForHash().putAll(key, fields);
            redisTemplate.expire(key, ttlFor(session).toSeconds(), TimeUnit.SECONDS);
            return true;
        } catch (RuntimeException ex) {
            log.warn("Cannot write media upload progress to Redis. uploadSessionId={}", uploadSessionId, ex);
            return false;
        }
    }

    void delete(String uploadSessionId) {
        try {
            redisTemplate.delete(key(uploadSessionId));
        } catch (RuntimeException ex) {
            log.warn("Cannot delete media upload progress from Redis. uploadSessionId={}", uploadSessionId, ex);
        }
    }

    private Duration ttlFor(MediaUploadSession session) {
        if (session.getExpiredAt() == null) {
            return DEFAULT_TTL;
        }

        Duration ttl = Duration.between(LocalDateTime.now(), session.getExpiredAt()).plus(EXPIRY_GRACE);
        if (ttl.isNegative() || ttl.compareTo(MIN_TTL) < 0) {
            return MIN_TTL;
        }
        return ttl;
    }

    private String key(String uploadSessionId) {
        return KEY_PREFIX + uploadSessionId;
    }

    private Long parseLong(Object value) {
        String stringValue = stringValue(value);
        if (stringValue == null) {
            return null;
        }
        try {
            return Long.parseLong(stringValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Integer parseInteger(Object value) {
        String stringValue = stringValue(value);
        if (stringValue == null) {
            return null;
        }
        try {
            return Integer.parseInt(stringValue);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private MediaUploadSessionStatus parseStatus(Object value) {
        String stringValue = stringValue(value);
        if (stringValue == null) {
            return null;
        }
        try {
            return MediaUploadSessionStatus.valueOf(stringValue);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    private String stringValue(Object value) {
        if (value == null) {
            return null;
        }
        String stringValue = String.valueOf(value);
        return stringValue.isBlank() ? null : stringValue;
    }
}
