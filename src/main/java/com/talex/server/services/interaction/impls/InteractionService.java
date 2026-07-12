package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.WatchTimeRequest;
import com.talex.server.services.interaction.IInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService implements IInteractionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    @Async("interactionExecutor")
    @Override
    public void processTelemetry(UUID accountId, WatchTimeRequest request) {
        // 1. Lưu Tiến Trình xem vào Redis Hash
        String redisKey = "user:progress:" + accountId;
        redisTemplate.opsForHash().put(redisKey, request.getEpisodeId(), String.valueOf(request.getCurrentPosition()));

        // 2. Bắn dữ liệu thô vào Kafka
        long eventTimestamp = request.getTimestamp()
                .atZone(java.time.ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
        String rawMessage = String.format("%s,%s,%s,%s,%.2f,%d",
                request.getSessionId(),
                accountId.toString(),
                request.getEpisodeId(),
                request.getEvent(),
                request.getHeartbeatValue(),
                eventTimestamp);

        kafkaTemplate.send("watch-raw", request.getSessionId(), rawMessage);
    }
}
