package com.talex.server.schedulers;

import com.talex.server.services.coin.IMissionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.UUID;

@Component
@Slf4j
@RequiredArgsConstructor
public class MissionHeartbeatScheduler {

    private static final String HEARTBEAT_HASH_KEY = "mission:online_heartbeat";

    private final StringRedisTemplate stringRedisTemplate;
    private final IMissionService missionService;

    @Scheduled(fixedRate = 300000)
    public void aggregateOnlineHeartbeats() {
        Map<Object, Object> heartbeats = stringRedisTemplate
                .opsForHash()
                .entries(HEARTBEAT_HASH_KEY);

        heartbeats.forEach((accountIdKey, minutesValue) -> {
            try {
                UUID accountId = UUID.fromString(accountIdKey.toString());
                int minutes = Integer.parseInt(minutesValue.toString());

                missionService.distributeOnlineHeartbeat(accountId, minutes);
                stringRedisTemplate.opsForHash().delete(HEARTBEAT_HASH_KEY, accountIdKey);
            } catch (Exception exception) {
                log.error(
                        "Failed to aggregate mission heartbeat for account [{}] with value [{}]",
                        accountIdKey,
                        minutesValue,
                        exception
                );
            }
        });
    }
}
