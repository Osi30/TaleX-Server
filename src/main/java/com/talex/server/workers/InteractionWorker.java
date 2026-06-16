package com.talex.server.workers;

import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class InteractionWorker implements StreamListener<String, MapRecord<String, String, String>> {
    private final Sender questDBSender;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionStatRepository statRepository;

    @KafkaListener(topics = "interaction-log-topic", groupId = "questdb-group")
    public void consume(String message) {
        try {
            String[] parts = message.split(",");
            String accountId = parts[0];
            String episodeId = parts[1];
            String contentType = parts[2];
            String interactionType = parts[3];
            LocalDateTime timestamp = LocalDateTime.parse(parts[4]);

            questDBSender.table("interaction_logs")
                    .symbol("account_id", accountId)
                    .symbol("episode_id", episodeId)
                    .symbol("content_type", contentType)
                    .symbol("interaction_type", interactionType)
                    .at(Instant.from(timestamp.atZone(ZoneId.systemDefault()).toInstant()));
            questDBSender.flush();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            // Đọc gói dữ liệu Map từ bản ghi của Redis Stream phát ra
            Map<String, String> body = message.getValue();

            String monthYear = body.get("monthYear");
            String viewerId = body.get("viewerId");
            String episodeId = body.get("episodeId");
            String subscriptionId = body.get("subscriptionId");
            String creatorId = body.get("creatorId");
            String contentType = body.get("contentType");
            String interactionType = body.get("interactionType");

            // Xác định cờ Boolean cụ thể cần bật dựa theo loại tương tác nhận được
            boolean isLike = "LIKE".equals(interactionType);
            boolean isComment = "COMMENT".equals(interactionType);
            boolean isBookmark = "BOOKMARK".equals(interactionType);
            boolean isShare = "SHARE".equals(interactionType);

            // Thực thi ghi xuống PostgreSQL qua câu lệnh Native Query UPSERT bảo mật NoSQL-style
            statRepository.upsertInteractionFlags(
                    monthYear, subscriptionId, viewerId, episodeId, contentType,
                    creatorId, isLike, isComment, isBookmark, isShare
            );

            // Xác nhận với Redis bản ghi đã được xử lý
            redisTemplate.opsForStream().acknowledge("interaction:stream", "pg-sync-group", message.getId());

        } catch (Exception e) {
            System.err.println("Lỗi xử lý đồng bộ bản ghi Redis Stream xuống PostgreSQL: " + e.getMessage());
        }
    }
}
