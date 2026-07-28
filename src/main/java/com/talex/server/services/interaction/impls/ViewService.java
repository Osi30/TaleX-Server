package com.talex.server.services.interaction.impls;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.interaction.request.ViewRequest;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.services.interaction.IViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.*;

@Service
@Slf4j
public class ViewService implements IViewService {
    private final JdbcTemplate questDbJdbcTemplate;
    private final StringRedisTemplate redisTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;

    public ViewService(
            @Qualifier("questDbJdbcTemplate") JdbcTemplate questDbJdbcTemplate,
            StringRedisTemplate redisTemplate,
            KafkaTemplate<String, String> kafkaTemplate,
            ObjectMapper objectMapper
    ) {
        this.questDbJdbcTemplate = questDbJdbcTemplate;
        this.redisTemplate = redisTemplate;
        this.kafkaTemplate = kafkaTemplate;
        this.objectMapper = objectMapper;
    }

    private static final RedisScript<String> BF_RESERVE_SCRIPT =
            new DefaultRedisScript<>("return redis.call('BF.RESERVE', KEYS[1], ARGV[1], ARGV[2])", String.class);

    private static final RedisScript<Long> BF_ADD_SCRIPT =
            new DefaultRedisScript<>("return redis.call('BF.ADD', KEYS[1], ARGV[1])", Long.class);

    private static final String VIEW_TOPIC = "talex-interaction.episode-viewed";
    private static final String KAFKA_TOPIC_SERIES_VIEW = "talex-interaction.series-viewed";
    private static final String BLOOM_WATCHED_PREFIX = "recommendation:bloom:watched:";
    private static final String BLOOM_INIT_FLAG_PREFIX = "recommendation:bloom:init:";

    @Async("interactionExecutor")
    @Override
    public void viewEpisode(ViewRequest viewRequest) {
        try {
            UUID accountId = viewRequest.getAccountId();
            String ipAddress = viewRequest.getIpAddress();
            String finalAccountId = (accountId == null
                    || accountId.toString().trim().isEmpty())
                    ? "" : accountId.toString();

            // Đóng gói JSON phẳng tối ưu băng thông mạng
            Map<String, Object> viewEvent = Map.of(
                    "ip_address", ipAddress != null ? ipAddress : "0.0.0.0",
                    "episode_id", viewRequest.getEpisodeId(),
                    "account_id", finalAccountId,
                    "session_id", viewRequest.getSessionId(),
                    "timestamp", Instant.now().toEpochMilli()
            );

            String messagePayload = objectMapper.writeValueAsString(viewEvent);
            kafkaTemplate.send(VIEW_TOPIC, viewRequest.getSessionId(), messagePayload);

        } catch (Exception e) {
            log.error("Lỗi khi gửi sự kiện View lên Kafka: ", e);
            throw new InteractionException(
                    InteractionErrorCode.KAFKA_PROCESSING_ERROR,
                    "Không thể ghi nhận lượt xem do sự cố hệ thống." + e.getMessage()
            );
        }
    }

    @Async("interactionExecutor")
    @Override
    public void trackSeriesViewAsync(String accountId, String seriesId) {
        if (seriesId == null || seriesId.trim().isEmpty()) return;

        try {
            // Nếu không phải khách vãng lai, lưu trực tiếp real-time vào Bloom Filter trên Redis
            if (!"anonymous".equals(accountId) && !accountId.trim().isEmpty()) {
                String bloomKey = BLOOM_WATCHED_PREFIX + accountId;
                String initFlagKey = BLOOM_INIT_FLAG_PREFIX + accountId;

                // 1. Khởi tạo Filter vật lý qua Lua Script nếu flag chưa tồn tại
                if (Boolean.FALSE.equals(redisTemplate.hasKey(initFlagKey))) {
                    try {
                        redisTemplate.execute(BF_RESERVE_SCRIPT, Collections.singletonList(bloomKey), "0.01", "20000");
                        redisTemplate.opsForValue().set(initFlagKey, "true", Duration.ofDays(30));
                    } catch (Exception e) {
                        log.debug("Bloom filter đã tồn tại từ trước hoặc được khởi tạo song song: {}", e.getMessage());
                    }
                }

                // 2. Nạp phần tử vào Bloom Filter thông qua Lua Script (Tránh hoàn toàn lỗi ByteArrayOutput)
                Long result = redisTemplate.execute(BF_ADD_SCRIPT, Collections.singletonList(bloomKey), seriesId);
                log.info("[ASYNC BLOOM] Nạp series {} cho user {}. Kết quả mã trả về: {}", seriesId, accountId, result);

                // 3. Đẩy tin nhắn thông báo lên cụm Kafka
                Map<String, Object> eventPayload = new HashMap<>();
                eventPayload.put("account_id", accountId);
                eventPayload.put("series_id", seriesId);
                eventPayload.put("timestamp", System.currentTimeMillis());

                String messageJson = objectMapper.writeValueAsString(eventPayload);
                kafkaTemplate.send(KAFKA_TOPIC_SERIES_VIEW, accountId, messageJson);
            }
        } catch (Exception e) {
            log.error("Lỗi khi xử lý lưu vết xem series bất đồng bộ: {}", e.getMessage());
        }
    }

    /**
     * Đảm bảo bloom filter cho account tồn tại
     */
    @Override
    public void ensureBloomFilterInitialized(String accountId) {
        String initFlagKey = BLOOM_INIT_FLAG_PREFIX + accountId;
        if (Boolean.FALSE.equals(redisTemplate.hasKey(initFlagKey))) {
            log.warn("[BLOOM FALLBACK] Bộ lọc trống. Đang nạp lại lịch sử từ bảng mới của QuestDB cho user: {}", accountId);
            String bloomKey = BLOOM_WATCHED_PREFIX + accountId;

            try {
                redisTemplate.execute(BF_RESERVE_SCRIPT, Collections.singletonList(bloomKey), "0.01", "20000");
            } catch (Exception e) {
                // Bỏ qua nếu cấu trúc vật lý đã tồn tại sẵn
            }

            List<String> historicallyWatched = fetchAllWatchedSeriesFromQuestDB(accountId);
            if (!historicallyWatched.isEmpty()) {
                // Nạp tuần tự an toàn qua Lua Script
                for (String sId : historicallyWatched) {
                    redisTemplate.execute(BF_ADD_SCRIPT, Collections.singletonList(bloomKey), sId);
                }
                log.info("[BLOOM FALLBACK] Đồng bộ thành công {} bộ truyện từ QuestDB vào Redis Bloom.", historicallyWatched.size());
            }
            redisTemplate.opsForValue().set(initFlagKey, "true", Duration.ofDays(30));
        }
    }

    /**
     * Quét dữ liệu từ bảng mới 'view_series_logs'
     */
    private List<String> fetchAllWatchedSeriesFromQuestDB(String accountId) {
        String sql = "SELECT series_id FROM view_series_logs WHERE account_id = ? AND series_id IS NOT NULL PARTITION BY series_id";
        try {
            return questDbJdbcTemplate.query(
                    sql, (rs, rowNum) -> rs.getString("series_id"), accountId);
        } catch (Exception e) {
            log.error("Thất bại khi quét bảng view_series_logs trên QuestDB: {}", e.getMessage());
            return Collections.emptyList();
        }
    }
}