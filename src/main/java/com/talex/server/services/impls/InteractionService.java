package com.talex.server.services.impls;

import com.talex.server.dtos.requests.interaction.InteractionRequest;
import com.talex.server.dtos.requests.interaction.WatchTimeRequest;
import com.talex.server.services.IInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService implements IInteractionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;

    private final DateTimeFormatter monthYearFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());

    @Async("interactionExecutor")
    @Override
    public void processInteraction(UUID accountId, InteractionRequest request) {
        String timestampStr = String.valueOf(request.getTimestamp());

        // Gửi log thô qua Kafka (QuestDB tiêu thụ bằng cổng mạng TCP)
        String kafkaMessage = String.format("%s,%s,%s,%s,%s",
                request.getSessionId(), accountId, request.getEpisodeId(), request.getInteractionType(), timestampStr);
        kafkaTemplate.send("interaction-log-topic", kafkaMessage);

        // Tích lũy trên Redis Stream
        String monthYear = monthYearFormatter.format(request.getTimestamp());

        // Tạo một gói dữ liệu Map phẳng
        Map<String, String> streamBody = new HashMap<>();
        streamBody.put("sessionId", request.getSessionId());
        streamBody.put("timestamp", timestampStr);
        streamBody.put("monthYear", monthYear);
        streamBody.put("viewerId", accountId.toString());
        streamBody.put("episodeId", request.getEpisodeId());
        streamBody.put("interactionType", request.getInteractionType().name());

        // Đẩy gói dữ liệu vào ống dẫn Redis Stream có tên là "interaction:stream"
        redisTemplate.opsForStream().add("interaction:stream", streamBody);
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
        String rawMessage = String.format("%s,%s,%s,%d,%d",
                request.getSessionId(),
                accountId.toString(),
                request.getEpisodeId(),
                request.getDuration(),
                eventTimestamp);

        kafkaTemplate.send("watch-raw", request.getSessionId(), rawMessage);
    }
}
