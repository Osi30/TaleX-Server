package com.talex.server.services.impls;

import com.talex.server.dtos.requests.InteractionRequest;
import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import com.talex.server.services.IInteractionService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class InteractionService implements IInteractionService {
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final SubscriptionStatRepository subscriptionStatRepository;
    private final StringRedisTemplate redisTemplate;

    private final DateTimeFormatter monthYearFormatter = DateTimeFormatter
            .ofPattern("yyyy-MM").withZone(ZoneId.systemDefault());

    @Override
    public void processInteraction(UUID accountId, InteractionRequest request) {
        String timestampStr = String.valueOf(request.getTimestamp());

        // Gửi log thô qua Kafka (QuestDB tiêu thụ bằng cổng mạng TCP)
        String kafkaMessage = String.format("%s,%s,%s,%s,%s",
                accountId, request.getEpisodeId(), request.getContentType(), request.getInteractionType(), timestampStr);
        kafkaTemplate.send("interaction-log-topic", kafkaMessage);

        // Tích lũy trên Redis Stream
        String monthYear = monthYearFormatter.format(request.getTimestamp());
        String episodeId = request.getEpisodeId();


        // 1. Lấy Creator ID từ Redis Cache
        String cacheCreatorKey = "cache:ep:creator:" + episodeId;
        String creatorId = redisTemplate.opsForValue().get(cacheCreatorKey);
        if (creatorId == null) {
            creatorId = subscriptionStatRepository.findCreatorIdByEpisodeId(episodeId);
            redisTemplate.opsForValue().set(cacheCreatorKey, creatorId);
        }

        // 2. Lấy Active Subscription ID từ Redis Cache
        String cacheSubKey = "cache:user:sub:" + accountId;
        String subscriptionId = redisTemplate.opsForValue().get(cacheSubKey);
        if (subscriptionId == null) {
            subscriptionId = subscriptionStatRepository
                    .findActiveAccountSubByAccountId(accountId, request.getTimestamp());
            redisTemplate.opsForValue().set(cacheSubKey, subscriptionId, Duration.ofHours(2));
        }

        // Tạo một gói dữ liệu Map phẳng để ném vào Redis Stream
        Map<String, String> streamBody = new HashMap<>();
        streamBody.put("monthYear", monthYear);
        streamBody.put("viewerId", accountId.toString());
        streamBody.put("episodeId", request.getEpisodeId());
        streamBody.put("subscriptionId", subscriptionId);
        streamBody.put("creatorId", creatorId);
        streamBody.put("contentType", request.getContentType().toString());
        streamBody.put("interactionType", request.getInteractionType().name());

        // Đẩy thẳng gói dữ liệu này vào ống dẫn Redis Stream có tên là "interaction:stream"
//        redisTemplate.opsForStream().add("interaction:stream", streamBody);
    }
}
