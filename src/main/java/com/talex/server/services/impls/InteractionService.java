package com.talex.server.services.impls;

import com.talex.server.dtos.requests.interaction.InteractionRequest;
import com.talex.server.dtos.requests.interaction.WatchTimeRequest;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.AccountInteractionRepository;
import com.talex.server.services.IInteractionService;
import lombok.RequiredArgsConstructor;
import org.jetbrains.annotations.NotNull;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService implements IInteractionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final AccountInteractionRepository accountInteractionRepository;

    @Async("interactionExecutor")
    @Override
    public void processInteraction(UUID accountId, InteractionRequest request) {
        String timestampStr = String.valueOf(request.getTimestamp());

        // Gửi log thô qua Kafka (QuestDB tiêu thụ bằng cổng mạng TCP)
        String kafkaMessage = String.format("%s,%s,%s,%s,%s",
                request.getSessionId(), accountId, request.getEpisodeId(), request.getInteractionType(), timestampStr);
        kafkaTemplate.send("interaction-log-topic", request.getSessionId(), kafkaMessage);

        // Tích lũy trên Redis
        String redisKey = String.format("interact:hash:%s:%s", accountId, request.getEpisodeId());

        Map<String, String> fields = getStringStringMap(
                accountId, request, timestampStr);

        redisTemplate.opsForHash().putAll(redisKey, fields);
        redisTemplate.expire(redisKey, Duration.ofHours(1));
    }

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

    @NotNull
    private static Map<String, String> getStringStringMap(UUID accountId, InteractionRequest request, String timestampStr) {
        Map<String, String> fields = new HashMap<>();
        fields.put("session_id", request.getSessionId());
        fields.put("account_id", accountId.toString());
        fields.put("episode_id", request.getEpisodeId());
        fields.put("last_updated", timestampStr);

        switch (request.getInteractionType()) {
            case LIKE -> fields.put("is_like", "true");
            case UNLIKE -> fields.put("is_like", "false");
            case BOOKMARK -> fields.put("is_bookmark", "true");
            case UNBOOKMARK -> fields.put("is_bookmark", "false");
            case COMMENT -> fields.put("is_comment", "true");
            case SHARE -> fields.put("is_share", "true");
        }
        return fields;
    }

    @Override
    public void handleInteraction(UUID accountId, InteractionRequest request) {
        try {
            accountInteractionRepository.upsertOrDeleteInteraction(
                    accountId,
                    request.getEpisodeId(),
                    request.getInteractionType().toString()
            );
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.SAVING_DATABASE_ERROR,
                    "Lỗi Interaction khi lưu xuống DB");
        }
    }
}
