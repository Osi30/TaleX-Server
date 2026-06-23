package com.talex.server.workers;

import com.talex.server.enums.ContentType;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.records.EpisodeDetails;
import com.talex.server.repositories.WatchSessionRepository;
import com.talex.server.repositories.subscription.SubscriptionStatRepository;
import com.talex.server.utils.ValidationUtils;
import io.questdb.client.Sender;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class InteractionWorker {
    private final Sender questDBSender;
    private final StringRedisTemplate redisTemplate;
    private final SubscriptionStatRepository statRepository;
    private final WatchSessionRepository watchSessionRepository;

    private final DateTimeFormatter monthYearFormatter = DateTimeFormatter.ofPattern("yyyy-MM");

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
            String event = parts[3];
            double duration = Double.parseDouble(parts[4]);
            long eventTimestamp = Long.parseLong(parts[5]);

            questDBSender.table("watch_time_raw_logs")
                    .symbol("session_id", sessionId)
                    .symbol("viewer_id", accountId)
                    .symbol("episode_id", episodeId)
                    .symbol("event", event)
                    .doubleColumn("duration", duration)
                    .at(Instant.ofEpochMilli(eventTimestamp));
            // Production xóa dòng này
            questDBSender.flush();
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
            double watchDuration = Double.parseDouble(parts[3]);
            int heartbeatCount = Integer.parseInt(parts[4]);
            long startTimeMs = Long.parseLong(parts[5]);
            long endTimeMs = Long.parseLong(parts[6]);

            LocalDateTime startTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(startTimeMs), ZoneId.systemDefault());
            LocalDateTime currentEndTime = LocalDateTime.ofInstant(
                    Instant.ofEpochMilli(endTimeMs), ZoneId.systemDefault());

            // Thông tin Metadata của tập phim/truyện
            EpisodeDetails episodeDetails = resolveEpisodeDetails(episodeId);

            // 3. Tiến hành ghi nhận/cộng dồn thời gian vào PostgreSQL
            watchSessionRepository.upsertWatchSession(
                    sessionId,
                    UUID.fromString(accountId),
                    episodeId,
                    episodeDetails.creatorId(),
                    watchDuration,
                    episodeDetails.totalDuration(),
                    heartbeatCount,
                    startTime,
                    currentEndTime
            );
        } catch (Exception e) {
            throw new InteractionException(InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "[Kafka Save to Database Worker Error] Nội dung: " + e.getMessage());
        }
    }

    @KafkaListener(topics = "interaction-log-topic", groupId = "postgres-interaction-group")
    public void consumeForPostgresSQL(String message) {
        try {
            String[] parts = message.split(",");
            String sessionId = parts[0];
            String viewerId = parts[1];
            String episodeId = parts[2];
            String interactionType = parts[3];
            String timestampStr = parts[4];

            LocalDateTime timestamp = LocalDateTime.parse(parts[4]);
            String monthYear = timestamp.format(monthYearFormatter);

            // Xác thực và tìm kiếm mã Gói đăng ký (SubscriptionId)
            String subscriptionId = resolveSubscriptionId(viewerId, timestampStr);
            // Không có gói đăng kí thì không cần lưu nữa
            if (ValidationUtils.isNullOrEmpty(subscriptionId)) return;

            // Định danh Episode Meta (Creator, Duration, Type) thông qua Cache/DB
            EpisodeDetails details = resolveEpisodeDetails(episodeId);

            // Xác định cờ Boolean cụ thể cần bật dựa theo loại tương tác nhận được
            boolean isLike = "LIKE".equals(interactionType);
            boolean isComment = "COMMENT".equals(interactionType);
            boolean isBookmark = "BOOKMARK".equals(interactionType);
            boolean isShare = "SHARE".equals(interactionType);

            statRepository.upsertInteractionFlags(
                    monthYear, subscriptionId, viewerId, episodeId,
                    details.contentType().toString(), details.creatorId(),
                    isLike, isComment, isBookmark, isShare, sessionId,
                    details.totalDuration(), interactionType
            );

        } catch (InteractionException e) {
            if (e.getErrorCode() != InteractionErrorCode.WORKER_EPISODE_NOT_FOUND &&
                    e.getErrorCode() != InteractionErrorCode.WORKER_ACTIVE_SUB_NOT_FOUND) {
                // Các lỗi nghiệp vụ khác thuộc về hạ tầng (ví dụ: WORKER_DATABASE_UPSERT_FAILED)
                throw new InteractionException(InteractionErrorCode.WORKER_PROCESSING_ERROR,
                        "[Worker Error] Nội dung: %s%n" + e.getMessage());
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
            double totalDuration = episodeDetails.totalDuration() == null ? 0 : episodeDetails.totalDuration();
            String dataToCache = episodeDetails.creatorId() + "|" + totalDuration + "|" + episodeDetails.contentType().toString();
            redisTemplate.opsForValue().set(cacheKey, dataToCache, Duration.ofHours(24));
            return episodeDetails;
        } else {

            // Giải nén dữ liệu từ Cache Hit
            String[] parts = cachedData.split("\\|");
            return new EpisodeDetails(parts[0], Double.parseDouble(parts[1]), ContentType.valueOf(parts[2]));
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
