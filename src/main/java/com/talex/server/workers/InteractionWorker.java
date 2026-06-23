package com.talex.server.workers;

import com.talex.server.enums.ContentType;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.records.EpisodeDetails;
import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import com.talex.server.utils.ValidationUtils;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.stream.StreamListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionWorker implements StreamListener<String, MapRecord<String, String, String>> {
    private final Sender questDBSender;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionStatRepository statRepository;

    // Kafka pipeline for user interaction
    @KafkaListener(topics = "interaction-log-topic", groupId = "questdb-interaction-group")
    public void consume(String message) {
        try {
            String[] parts = message.split(",");
            String sessionId = parts[0];
            String accountId = parts[1];
            String episodeId = parts[2];
            String interactionType = parts[3];
            LocalDateTime timestamp = LocalDateTime.parse(parts[4]);

            questDBSender.table("interaction_logs")
                    .symbol("session_id", sessionId)
                    .symbol("account_id", accountId)
                    .symbol("episode_id", episodeId)
                    .symbol("interaction_type", interactionType)
                    .at(Instant.from(timestamp.atZone(ZoneId.systemDefault()).toInstant()));
            // Production xóa dòng này
            questDBSender.flush();

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "[Kafka Interaction Worker Error] Nội dung: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "watch-raw", groupId = "questdb-watch-group")
    public void consumeRawLog(String message) {
        try {
            String[] parts = message.split(",");
            String sessionId = parts[0];
            String accountId = parts[1];
            String episodeId = parts[2];
            long duration = Long.parseLong(parts[3]);

//            questDBSender.table("watch_time_raw_logs")
//                    .symbol("session_id", sessionId)
//                    .symbol("viewer_id", accountId)
//                    .symbol("episode_id", episodeId)
//                    .longColumn("duration", duration)
//                    .atNow();
//            // Production xóa dòng này
//            questDBSender.flush();
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "[Kafka Watch Worker Error] Nội dung: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "watch-summary", groupId = "postgres-watch-group")
    public void consumeSummaryLog(String message) {
        try {
            String[] parts = message.split(",");
            String sessionId = parts[0];
            String accountId = parts[1];
            String episodeId = parts[2];
            long totalDuration = Long.parseLong(parts[3]);

            String monthYear = java.time.format.DateTimeFormatter.ofPattern("yyyy-MM")
                    .format(java.time.LocalDateTime.now());

            // Thông tin Metadata của tập phim/truyện
            EpisodeDetails episodeDetails = resolveEpisodeDetails(episodeId);

            // Thông tin Gói đăng ký đang hoạt động của User
            String subscriptionId = resolveSubscriptionId(accountId, java.time.LocalDateTime.now().toString());
            // Không có gói đăng kí thì không cần lưu nữa
            if (ValidationUtils.isNullOrEmpty(subscriptionId)) {
                return;
            }

            // 3. Tiến hành ghi nhận/cộng dồn thời gian vào PostgreSQL
            statRepository.upsertWatchTime(
                    monthYear,
                    subscriptionId,
                    accountId,
                    episodeId,
                    episodeDetails.contentType().name(),
                    episodeDetails.creatorId(),
                    totalDuration,
                    sessionId
            );
            log.info("Chốt sổ thành công cho Session! User {} xem {} được {} giây.", accountId, episodeId, totalDuration);

        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "[Kafka Save to Database Worker Error] Nội dung: " + e.getMessage());
        }
    }

    // Redis Stream for user interaction
    @Override
    public void onMessage(MapRecord<String, String, String> message) {
        try {
            // Đọc gói dữ liệu Map từ bản ghi của Redis Stream phát ra
            Map<String, String> body = message.getValue();

            String sessionId = body.get("sessionId");
            String timestamp = body.get("timestamp");
            String monthYear = body.get("monthYear");
            String viewerId = body.get("viewerId");
            String episodeId = body.get("episodeId");
            String interactionType = body.get("interactionType");

            // Định danh Episode Meta (Creator, Duration, Type) thông qua Cache/DB
            EpisodeDetails details = resolveEpisodeDetails(episodeId);

            // Xác thực và tìm kiếm mã Gói đăng ký (SubscriptionId)
            String subscriptionId = resolveSubscriptionId(viewerId, timestamp);
            // Không có gói đăng kí thì không cần lưu nữa
            if (ValidationUtils.isNullOrEmpty(subscriptionId)) {
                redisTemplate.opsForStream().acknowledge("interaction:stream", "pg-sync-group", message.getId());
            }

            // Xác định cờ Boolean cụ thể cần bật dựa theo loại tương tác nhận được
            boolean isLike = "LIKE".equals(interactionType);
            boolean isComment = "COMMENT".equals(interactionType);
            boolean isBookmark = "BOOKMARK".equals(interactionType);
            boolean isShare = "SHARE".equals(interactionType);

            if (!ValidationUtils.isNullOrEmpty(subscriptionId)) {
                statRepository.upsertInteractionFlags(
                        monthYear, subscriptionId, viewerId, episodeId, details.contentType().toString(),
                        details.creatorId(), isLike, isComment, isBookmark, isShare, sessionId, details.totalDuration()
                );

                // Xác nhận với Redis bản ghi đã được xử lý
                redisTemplate.opsForStream().acknowledge("interaction:stream", "pg-sync-group", message.getId());
            }

        } catch (InteractionException e) {
            if (e.getErrorCode() == InteractionErrorCode.WORKER_EPISODE_NOT_FOUND ||
                    e.getErrorCode() == InteractionErrorCode.WORKER_ACTIVE_SUB_NOT_FOUND) {

                log.warn("[POISON PILL SKIPPED] Bỏ qua dữ liệu lỗi từ client để tránh tắc nghẽn. ID: {} | Lý do: {}", message.getId(), e.getMessage());
                redisTemplate.opsForStream().acknowledge("interaction:stream", "pg-sync-group", message.getId());
            } else {
                // Các lỗi nghiệp vụ khác thuộc về hạ tầng (ví dụ: WORKER_DATABASE_UPSERT_FAILED)
                log.info("[INFRASTRUCTURE ERROR] Lỗi DB tạm thời, giữ lại trong PEL: {}", e.getMessage());
            }
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.WORKER_PROCESSING_ERROR,
                    "[Worker Error] Nội dung: %s%n" + e.getMessage());
        }
    }

    private EpisodeDetails resolveEpisodeDetails(String episodeId) {
        String cacheKey = "cache:ep:details:" + episodeId;
        String cachedData = redisTemplate.opsForValue().get(cacheKey);

        if (ValidationUtils.isNullOrEmpty(cachedData)) {
            EpisodeDetails episodeDetails = statRepository.findEpisodeDetails(episodeId);
            if (episodeDetails == null) {
                throw new InteractionException(InteractionErrorCode.WORKER_EPISODE_NOT_FOUND,
                        "Episode " + episodeId + " không tồn tại để lấy metadata.");
            }

            // Nén dữ liệu đẩy vào Redis
            long totalDuration = episodeDetails.totalDuration() == null ? 0 : episodeDetails.totalDuration();
            String dataToCache = episodeDetails.creatorId() + "|" + totalDuration + "|" + episodeDetails.contentType().toString();
            redisTemplate.opsForValue().set(cacheKey, dataToCache, Duration.ofHours(24));
            return episodeDetails;
        } else {

            // Giải nén dữ liệu từ Cache Hit
            String[] parts = cachedData.split("\\|");
            return new EpisodeDetails(parts[0], Long.parseLong(parts[1]), ContentType.valueOf(parts[2]));
        }
    }

    private String resolveSubscriptionId(String viewerId, String timestampStr) {
        String cacheSubKey = "cache:user:sub:" + viewerId;
        String subscriptionId = redisTemplate.opsForValue().get(cacheSubKey);

        if (ValidationUtils.isNullOrEmpty(subscriptionId)) {
            LocalDateTime timestamp = LocalDateTime.parse(timestampStr);
            subscriptionId = statRepository.findActiveAccountSubByAccountId(UUID.fromString(viewerId), timestamp);

            if (ValidationUtils.isNullOrEmpty(subscriptionId)) return null;
            redisTemplate.opsForValue().set(cacheSubKey, subscriptionId, Duration.ofHours(2));
        }
        return subscriptionId;
    }
}
