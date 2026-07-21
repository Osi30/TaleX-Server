package com.talex.server.services.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.recommend.RankRequestPayload;
import com.talex.server.dtos.recommend.RankResultItem;
import com.talex.server.repositories.mongo.SeriesRecommendationRepository;
import com.talex.server.services.RecommendationService;
import com.talex.server.services.interaction.IViewService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.*;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final StringRedisTemplate redisTemplate;
    private final SeriesRecommendationRepository seriesRecommendationRepository;
    private final JdbcTemplate questDbJdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IViewService viewService;

    public RecommendationServiceImpl(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Qualifier("questDbJdbcTemplate") JdbcTemplate questDbJdbcTemplate,
            SeriesRecommendationRepository seriesRecommendationRepository,
            IViewService viewService
    ) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.questDbJdbcTemplate = questDbJdbcTemplate;
        this.seriesRecommendationRepository = seriesRecommendationRepository;
        this.viewService = viewService;
    }

    private static final String REDIS_KEY_PREFIX = "watch:top5:recent_series:";
    private static final String RECOMMENDATION_PREFIX = "recommendation:series:";
    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final Duration RECOMMENDATION_TTL = Duration.ofDays(7);
    private static final String AI_SERVICE_RANK_URL = "http://localhost:8000/api/v1/recommendations/rank";
    private static final String SESSION_KEY_PREFIX = "recommendation:session:";
    private static final String KAFKA_TOPIC_DISPLAY = "recommendation-display-log";
    private static final String BLOOM_WATCHED_PREFIX = "recommendation:bloom:watched:";
    private static final RedisScript<Long> BF_EXISTS_SCRIPT =
            new DefaultRedisScript<>("return redis.call('BF.EXISTS', KEYS[1], ARGV[1])", Long.class);

    /// Lấy danh sách gợi ý
    @Override
    public List<RankResultItem> getRecommendations(String accountId, List<String> seriesIds, String viewSessionId) {
        if (seriesIds == null || seriesIds.isEmpty()) return Collections.emptyList();

        // 1. Tách biệt lấy danh sách đã hiển thị trong phiên
        Set<String> alreadyShownIds = this.getAlreadyShownSessionIds(accountId, viewSessionId);

        // 2. Thực hiện hàm lọc phụ 1 (Theo Phiên)
        List<String> sessionFilteredCandidates = this.filterBySession(seriesIds, alreadyShownIds);
        if (sessionFilteredCandidates.isEmpty()) return Collections.emptyList();

        // 3. Thực hiện hàm lọc phụ 2 (Lịch sử xem vĩnh viễn)
        List<String> finalCandidates = this.filterByWatchedHistory(accountId, sessionFilteredCandidates);
        if (finalCandidates.isEmpty()) return Collections.emptyList();

        // 4. Bắn log hiển thị async phục vụ thống kê phiên
        trackSessionImpressionsAsync(accountId, viewSessionId, finalCandidates);

        // 5. Chuyển sang máy chấm điểm AI
        // return rankSeries(accountId, filteredCandidates);
        return List.of();
    }

    /// Lấy danh sách series người dùng mới xem (5)
    @Override
    public List<String> getRecentWatchedSeries(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String redisKey = REDIS_KEY_PREFIX + accountId;

        try {
            // 1. THỬ LẤY TỪ REDIS CACHE (Lấy từ index 0 đến 4)
            List<String> cachedSeries = redisTemplate.opsForList().range(redisKey, 0, 4);

            // 1.1 CÓ CACHE
            if (cachedSeries != null && !cachedSeries.isEmpty()) {
                return cachedSeries;
            }

            // 1.2. CACHE MISS -> TRUY VẤN QUESTDB
            List<String> dbSeries = fetchRecentSeriesFromQuestDB(accountId);

            // 3. NẾU CÓ DỮ LIỆU -> REBUILD LẠI CACHE TRÊN REDIS
            if (!dbSeries.isEmpty()) {
                // Xóa key cũ phòng trường hợp có rác/dữ liệu lệch
                redisTemplate.delete(redisKey);
                redisTemplate.opsForList().rightPushAll(redisKey, dbSeries);
                redisTemplate.expire(redisKey, CACHE_TTL);
            }

            return dbSeries;

        } catch (Exception e) {
            log.error("Lỗi khi lấy danh sách series gần nhất cho user {}: {}", accountId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /// Lấy danh sách các id tương tự (similarIds)
    @Override
    public List<String> getSimilarSeriesIds(String seriesId) {
        if (seriesId == null || seriesId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String redisKey = RECOMMENDATION_PREFIX + seriesId;

        try {
            // 1. THỬ LẤY TỪ REDIS CACHE (Dạng JSON String)
            String cachedJson = redisTemplate.opsForValue().get(redisKey);

            // 1.1 Nếu có Cache -> Parse JSON trả về luôn
            if (cachedJson != null && !cachedJson.isEmpty()) {
                return objectMapper.readValue(cachedJson, new TypeReference<List<String>>() {
                });
            }

            // 1.2 Nếu Cache Miss hoặc hết hạn -> Truy vấn sang MongoDB
            return seriesRecommendationRepository.findById(seriesId)
                    .map(recommendation -> {
                        List<String> similarIds = recommendation.getSimilarIds();

                        if (similarIds != null && !similarIds.isEmpty()) {
                            // 2. Lưu redis
                            try {
                                String similarIdsJson = objectMapper.writeValueAsString(similarIds);
                                redisTemplate.opsForValue().set(redisKey, similarIdsJson, RECOMMENDATION_TTL);
                                log.info("Tái cấu trúc (rebuild) cache Redis thành công cho seriesId={}", seriesId);
                            } catch (Exception ex) {
                                log.error("Lỗi khi đồng bộ ngược data lên Redis cho seriesId={}: {}", seriesId, ex.getMessage());
                            }
                            return similarIds;
                        }
                        return Collections.<String>emptyList();
                    })
                    .orElse(Collections.emptyList());

        } catch (Exception e) {
            log.error("Thất bại khi xử lý tìm similarIds cho seriesId={}: {}", seriesId, e.getMessage(), e);
            return Collections.emptyList();
        }
    }

    /// Chấm điểm Series dựa trên hồ sơ người dùng
    @Override
    public List<RankResultItem> rankSeries(String accountId, List<String> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Collections.emptyList();
        }

        // Không có user tĩnh thì trả về list gốc chưa sort
        if (accountId == null || accountId.trim().isEmpty()) {
            return List.of();
        }

        try {
            // 1. Chuẩn bị Payload đóng gói gửi đi
            RankRequestPayload payload = new RankRequestPayload(accountId, seriesIds);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<RankRequestPayload> entity = new HttpEntity<>(payload, headers);

            // 2. Gọi HTTP POST đồng bộ sang Python FastAPI
            RestTemplate isolatedRestTemplate = new RestTemplate();

            ResponseEntity<List<RankResultItem>> response = isolatedRestTemplate.exchange(
                    AI_SERVICE_RANK_URL,
                    org.springframework.http.HttpMethod.POST,
                    entity,
                    new org.springframework.core.ParameterizedTypeReference<List<RankResultItem>>() {
                    }
            );

            if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
                List<RankResultItem> rankedItems = response.getBody();
                rankedItems.forEach(item -> log.info("AI chấm điểm -> Series: {}, Score: {}", item.getSeriesId(), item.getScore()));
                return rankedItems;
            }

        } catch (Exception e) {
            log.error("Lỗi kết nối hoặc xử lý tại TaleX AI Service: {}. Kích hoạt chế độ Fallback!", e.getMessage());
        }

        // Trả lại list gốc của tầng Retrieval để client không bị trắng màn hình
        return List.of();
    }

    @Async
    public void trackSessionImpressionsAsync(String accountId, String viewSessionId, List<String> displayedIds) {
        if (displayedIds == null || displayedIds.isEmpty()) return;
        try {
            String sessionRedisKey = SESSION_KEY_PREFIX + accountId + ":" + viewSessionId;

            // 1. Đẩy vào Redis Set cấu trúc phiên
            redisTemplate.opsForSet().add(sessionRedisKey, displayedIds.toArray(new String[0]));
            redisTemplate.expire(sessionRedisKey, Duration.ofHours(2));

            // 2. Bắn gói tin JSON sang cho cụm Kafka Broker phục vụ thống kê
            Map<String, Object> kafkaLog = new HashMap<>();
            kafkaLog.put("accountId", accountId);
            kafkaLog.put("viewSessionId", viewSessionId);
            kafkaLog.put("displayedSeriesIds", displayedIds);
            kafkaLog.put("timestamp", System.currentTimeMillis());

            String messagePayload = objectMapper.writeValueAsString(kafkaLog);
            kafkaTemplate.send(KAFKA_TOPIC_DISPLAY, accountId, messagePayload);

        } catch (Exception e) {
            log.error("[ASYNC ERROR] Lỗi luồng phụ ghi log hiển thị: {}", e.getMessage());
        }
    }

    /**
     * Lấy các series đã được hiện trong phiên
     */
    private Set<String> getAlreadyShownSessionIds(String accountId, String viewSessionId) {
        String sessionRedisKey = SESSION_KEY_PREFIX + accountId + ":" + viewSessionId;
        Set<String> alreadyShownIds = redisTemplate.opsForSet().members(sessionRedisKey);
        return alreadyShownIds != null ? alreadyShownIds : Collections.emptySet();
    }


    /**
     * Lọc các series đã được hiện trong phiên
     */
    private List<String> filterBySession(List<String> seriesIds, Set<String> alreadyShownIds) {
        List<String> filtered = new ArrayList<>();
        for (String sId : seriesIds) {
            if (!alreadyShownIds.contains(sId)) {
                filtered.add(sId);
            } else {
                log.info("[LỌC PHIÊN 1] Loại bỏ series {} do đã hiển thị trong phiên này", sId);
            }
        }
        return filtered;
    }


    /**
     * Lọc các series đã được xem rồi
     */
    private List<String> filterByWatchedHistory(String accountId, List<String> candidates) {
        if ("anonymous".equals(accountId) || "guest_user".equals(accountId) || accountId == null) {
            return candidates;
        }

        viewService.ensureBloomFilterInitialized(accountId);

        String bloomKey = BLOOM_WATCHED_PREFIX + accountId;
        List<String> filtered = new ArrayList<>();

        for (String sId : candidates) {
            Long exists = redisTemplate.execute(BF_EXISTS_SCRIPT, Collections.singletonList(bloomKey), sId);

            if (exists != null && exists == 1L) {
                log.info("[LỌC VĨNH VIỄN] Đã chặn hiển thị bộ truyện đã xem: {}", sId);
            } else {
                filtered.add(sId);
            }
        }
        return filtered;
    }

    /**
     * Hàm helper thực thi query trực tiếp trên QuestDB qua JDBC
     */
    private List<String> fetchRecentSeriesFromQuestDB(String accountId) {
        String sql = "SELECT series_id FROM watch_session_logs " +
                "WHERE account_id = ? AND series_id IS NOT NULL " +
                "LATEST ON timestamp PARTITION BY series_id " +
                "ORDER BY timestamp DESC " +
                "LIMIT 5";

        try {
            return questDbJdbcTemplate.query(
                    sql, (rs, rowNum) -> rs.getString("series_id"), accountId);
        } catch (Exception e) {
            log.error("Thất bại khi query QuestDB cho accountId {}: {}", accountId, e.getMessage());
            return Collections.emptyList();
        }
    }
}
