package com.talex.server.services.coin.impls;

import com.talex.server.dtos.responses.coin.AdSessionResponseDto;
import com.talex.server.exceptions.codes.CoinErrorCode;
import com.talex.server.exceptions.details.CoinException;
import com.talex.server.repositories.coin.MissionRepository;
import com.talex.server.services.coin.IMissionAdService;
import com.talex.server.services.coin.IMissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class MissionAdServiceImpl implements IMissionAdService {

    private static final String AD_SESSION_KEY_PREFIX = "mission:ad_session:";
    private static final int AD_SESSION_TTL_SECONDS = 90;
    private static final int MINIMUM_WATCH_SECONDS = 10;

    private final StringRedisTemplate stringRedisTemplate;
    private final IMissionService missionService;
    private final MissionRepository missionRepository;

    @Override
    public AdSessionResponseDto startAdSession(UUID accountId, String missionCode) {
        missionRepository.findByCode(missionCode)
                .filter(mission -> mission.isActive())
                .orElseThrow(() -> new CoinException(CoinErrorCode.MISSION_NOT_FOUND));

        UUID sessionId = UUID.randomUUID();
        long startedAt = System.currentTimeMillis();
        String value = accountId + ":" + missionCode + ":" + startedAt;

        stringRedisTemplate.opsForValue().set(
                buildSessionKey(sessionId.toString()),
                value,
                Duration.ofSeconds(AD_SESSION_TTL_SECONDS)
        );

        log.info("Started mission ad session [{}] for account [{}] and mission [{}]",
                sessionId, accountId, missionCode);

        return AdSessionResponseDto.builder()
                .sessionId(sessionId.toString())
                .expiresInSeconds(AD_SESSION_TTL_SECONDS)
                .build();
    }

    @Override
    public void completeAdSession(UUID accountId, String sessionId) {
        String redisKey = buildSessionKey(sessionId);
        String value = stringRedisTemplate.opsForValue().get(redisKey);

        if (value == null) {
            throw new CoinException(CoinErrorCode.AD_SESSION_INVALID);
        }

        String[] parts = value.split(":");
        if (parts.length != 3) {
            stringRedisTemplate.delete(redisKey);
            throw new CoinException(CoinErrorCode.AD_SESSION_INVALID);
        }

        String storedAccountId = parts[0];
        String missionCode = parts[1];
        long startedAt = parseStartedAt(parts[2], redisKey);

        if (!storedAccountId.equals(accountId.toString())) {
            throw new CoinException(CoinErrorCode.AD_SESSION_INVALID);
        }

        long elapsedSeconds = (System.currentTimeMillis() - startedAt) / 1000;
        if (elapsedSeconds < MINIMUM_WATCH_SECONDS) {
            throw new CoinException(CoinErrorCode.AD_WATCH_TIME_TOO_SHORT);
        }

        stringRedisTemplate.delete(redisKey);
        missionService.addProgress(accountId, missionCode, 1);

        log.info("Completed mission ad session [{}] for account [{}] and mission [{}] after {} seconds",
                sessionId, accountId, missionCode, elapsedSeconds);
    }

    private long parseStartedAt(String startedAtValue, String redisKey) {
        try {
            return Long.parseLong(startedAtValue);
        } catch (NumberFormatException exception) {
            stringRedisTemplate.delete(redisKey);
            throw new CoinException(CoinErrorCode.AD_SESSION_INVALID);
        }
    }

    private String buildSessionKey(String sessionId) {
        return AD_SESSION_KEY_PREFIX + sessionId;
    }
}
