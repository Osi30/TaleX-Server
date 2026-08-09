package com.talex.server.services.recommend.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.mongo.UserStaticFeature;
import com.talex.server.dtos.recommend.*;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.repositories.mongo.SeriesRecommendationRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.mongo.IUserFeatureService;
import com.talex.server.services.recommend.RecommendationService;
import com.talex.server.services.recommend.SeriesChannelService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@Slf4j
public class RecommendationServiceImpl implements RecommendationService {
    private final String pythonApi;
    private final StringRedisTemplate redisTemplate;
    private final SeriesRecommendationRepository seriesRecommendationRepository;
    private final SeriesChannelService seriesChannelService;
    private final SeriesRepository seriesRepository;
    private final JdbcTemplate questDbJdbcTemplate;
    private final KafkaTemplate<String, String> kafkaTemplate;
    private final ObjectMapper objectMapper;
    private final IUserFeatureService userFeatureService;

    public RecommendationServiceImpl(
            @Value("${python.api}") String pythonApi, StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            KafkaTemplate<String, String> kafkaTemplate,
            @Qualifier("questDbJdbcTemplate") JdbcTemplate questDbJdbcTemplate,
            SeriesRecommendationRepository seriesRecommendationRepository,
            SeriesChannelService seriesChannelService,
            SeriesRepository seriesRepository,
            IUserFeatureService userFeatureService
    ) {
        this.pythonApi = pythonApi;
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.kafkaTemplate = kafkaTemplate;
        this.questDbJdbcTemplate = questDbJdbcTemplate;
        this.seriesRecommendationRepository = seriesRecommendationRepository;
        this.seriesChannelService = seriesChannelService;
        this.seriesRepository = seriesRepository;
        this.userFeatureService = userFeatureService;
    }

    private static final String REDIS_KEY_PREFIX = "watch:top5:recent_series:";
    private static final String RECOMMENDATION_PREFIX = "recommendation:series:";
    private static final String RECOMMENDATION_POOL_PREFIX = "recommendation:pool:";
    private static final String RECOMMENDATION_OFFSET_PREFIX = "recommendation:offset:";
    private static final String RECOMMENDATION_ALREADY_WATCHED_PREFIX = "recommendation:already_watched:";

    private static final Duration CACHE_TTL = Duration.ofDays(1);
    private static final Duration RECOMMENDATION_TTL = Duration.ofDays(7);
    private static final Duration RECOMMENDATION_POOL_TTL = Duration.ofHours(1);
    private static final Duration ALREADY_WATCHED_POOL_TTL = Duration.ofDays(1);

    private static final String AI_SERVICE_RANK_URL = "/api/v1/recommendations/rank";

    /// Lấy danh sách series từ các pool
    @Override
    public HomePoolsSeriesResponseDto getHomeFeedSeries(String accountId, HomeFeedRequestDto request) {
        String userIdStr = (accountId == null) ? "" : accountId.trim();
        if (request == null) {
            request = new HomeFeedRequestDto();
        }
        final HomeFeedRequestDto req = request;

        // 1. Kích hoạt lấy Global IDs Async (dùng làm blacklist cho Subscription)
        CompletableFuture<Set<String>> globalIdsFuture = CompletableFuture.supplyAsync(
                seriesChannelService::getAllGlobalIds
        ).exceptionally(ex -> {
            log.error("[HomeFeed] Lỗi lấy globalIds: {}", ex.getMessage());
            return Collections.emptySet();
        });

        // 2. Kích hoạt lấy 7 Pools độc lập cùng một lúc với limit riêng biệt
        CompletableFuture<List<String>> promotedFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getPromotedSeriesIds(userIdStr, req.getPromotedLimit())
        ).exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<String>> trendingFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getTrendingSeriesIds(userIdStr, req.getTrendingLimit())
        ).exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<String>> newReleasesFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getNewReleasesSeriesIds(userIdStr, req.getNewReleasesLimit())
        ).exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<String>> recentlyUpdatedFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getRecentlyUpdatedSeriesIds(userIdStr, req.getRecentlyUpdatedLimit())
        ).exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<String>> latestCommunityFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getLatestCommunityChoiceSeriesIds(userIdStr, req.getLatestCommunityChoiceLimit())
        ).exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<String>> communityChoiceFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getCommunityChoiceSeriesIds(userIdStr, req.getCommunityChoiceLimit())
        ).exceptionally(ex -> Collections.emptyList());

        CompletableFuture<List<String>> randomCategoryFuture = CompletableFuture.supplyAsync(
                () -> seriesChannelService.getRandomCategorySeriesIds(userIdStr, req.getRandomCategoryLimit())
        ).exceptionally(ex -> Collections.emptyList());

        // 3. Kênh Subscription
        CompletableFuture<List<String>> subscriptionFuture = globalIdsFuture.thenApplyAsync(
                globalIds -> seriesChannelService.getSubscribedCreatorsSeriesIds(userIdStr, globalIds, req.getSubscriptionLimit())
        ).exceptionally(ex -> Collections.emptyList());

        // 4. Chờ tất cả 8 Futures hoàn tất cùng lúc
        CompletableFuture.allOf(
                promotedFuture, trendingFuture, newReleasesFuture, recentlyUpdatedFuture,
                latestCommunityFuture, communityChoiceFuture, randomCategoryFuture, subscriptionFuture
        ).join();

        // 5. Trích xuất kết quả an toàn (Dùng join() hoặc getNow() vì allOf đã đảm bảo tất cả đã xong)
        List<String> promotedIds = promotedFuture.join();
        List<String> trendingIds = trendingFuture.join();
        List<String> newReleasesIds = newReleasesFuture.join();
        List<String> recentlyUpdatedIds = recentlyUpdatedFuture.join();
        List<String> latestCommunityIds = latestCommunityFuture.join();
        List<String> communityChoiceIds = communityChoiceFuture.join();
        List<String> randomCategoryIds = randomCategoryFuture.join();
        List<String> subscriptionIds = subscriptionFuture.join();

        // 6. Gom tất cả IDs unique để query DB 1 lần duy nhất
        Set<String> allUniqueIds = new LinkedHashSet<>();
        allUniqueIds.addAll(promotedIds);
        allUniqueIds.addAll(trendingIds);
        allUniqueIds.addAll(newReleasesIds);
        allUniqueIds.addAll(recentlyUpdatedIds);
        allUniqueIds.addAll(latestCommunityIds);
        allUniqueIds.addAll(communityChoiceIds);
        allUniqueIds.addAll(randomCategoryIds);
        allUniqueIds.addAll(subscriptionIds);

        if (allUniqueIds.isEmpty()) {
            return new HomePoolsSeriesResponseDto(
                    List.of(), List.of(), List.of(), List.of(),
                    List.of(), List.of(), List.of(), List.of()
            );
        }

        // Bắn sự kiện Async sang Kafka cho ImpressionWorker xử lý
        if (!userIdStr.isEmpty() && !"anonymous".equals(userIdStr) && !"guest_user".equals(userIdStr)) {
            sendHomeImpressionsAsync(userIdStr, new ArrayList<>(allUniqueIds));
        }

        // 7. Query PostgreSQL lấy dữ liệu Card
        List<SeriesCardResponseDto> fetchedCards = seriesRepository.findSeriesCardsByIds(
                allUniqueIds, SeriesStatus.PUBLISHED
        );

        Map<String, SeriesCardResponseDto> cardMap = fetchedCards.stream()
                .collect(Collectors.toMap(SeriesCardResponseDto::getSeriesId, card -> card, (c1, c2) -> c1));

        // 8. Build DTOs phân loại theo từng Pool
        List<SeriesCardResponseDto> promotedCards = mapIdsToDtos(promotedIds, cardMap);
        List<SeriesCardResponseDto> trendingCards = mapIdsToDtos(trendingIds, cardMap);
        List<SeriesCardResponseDto> newReleasesCards = mapIdsToDtos(newReleasesIds, cardMap);
        List<SeriesCardResponseDto> recentlyUpdatedCards = mapIdsToDtos(recentlyUpdatedIds, cardMap);
        List<SeriesCardResponseDto> latestCommunityCards = mapIdsToDtos(latestCommunityIds, cardMap);
        List<SeriesCardResponseDto> communityChoiceCards = mapIdsToDtos(communityChoiceIds, cardMap);
        List<SeriesCardResponseDto> randomCategoryCards = mapIdsToDtos(randomCategoryIds, cardMap);
        List<SeriesCardResponseDto> subscriptionCards = mapIdsToDtos(subscriptionIds, cardMap);

        return HomePoolsSeriesResponseDto.builder()
                .promoted(promotedCards)
                .trending(trendingCards)
                .newReleases(newReleasesCards)
                .recentlyUpdated(recentlyUpdatedCards)
                .latestCommunityChoice(latestCommunityCards)
                .communityChoice(communityChoiceCards)
                .randomCategory(randomCategoryCards)
                .accountSubscription(subscriptionCards)
                .build();
    }

    @Override
    public List<SeriesCardResponseDto> getPersonalizedRecommendations(
            String accountId,
            String sessionId,
            String pageType,
            int limit
    ) {
        String userIdStr = (accountId == null || accountId.trim().isEmpty()) ? "guest_user" : accountId.trim();
        String safeSessionId = (sessionId == null || sessionId.trim().isEmpty()) ? UUID.randomUUID().toString() : sessionId.trim();
        String safePageType = (pageType == null || pageType.trim().isEmpty()) ? "HOME" : pageType.trim().toUpperCase();
        int safeLimit = (limit <= 0) ? 12 : limit;

        // Key định danh Pool và Offset theo accountId + sessionId + pageType
        String redisPoolKey = RECOMMENDATION_POOL_PREFIX + userIdStr + ":" + safeSessionId + ":" + safePageType;
        String redisOffsetKey = RECOMMENDATION_OFFSET_PREFIX + userIdStr + ":" + safeSessionId + ":" + safePageType;

        // 1. Kiểm tra nếu Pool chưa tồn tại trong Redis -> Khởi tạo Pool & Reset Offset
        Boolean hasPool = redisTemplate.hasKey(redisPoolKey);

        if (Boolean.FALSE.equals(hasPool)) {
            initializeRecommendationPool(userIdStr, safeSessionId, safePageType, redisPoolKey);
            // Đặt offset ban đầu là 0
            redisTemplate.opsForValue().set(redisOffsetKey, "0", RECOMMENDATION_POOL_TTL);
        }

        // 2. Lấy offset hiện tại từ Redis (Mặc định là 0 nếu không tìm thấy)
        String currentOffsetStr = redisTemplate.opsForValue().get(redisOffsetKey);
        int currentOffset = (currentOffsetStr != null) ? Integer.parseInt(currentOffsetStr) : 0;

        // 3. Đọc dữ liệu từ Redis Pool theo dải [currentOffset -> currentOffset + limit - 1]
        List<String> pagedSeriesIds = redisTemplate.opsForList().range(
                redisPoolKey,
                currentOffset,
                currentOffset + safeLimit - 1
        );

        if (pagedSeriesIds == null || pagedSeriesIds.isEmpty()) {
            return Collections.emptyList(); // Đã xem hết Pool
        }

        // 4. Cập nhật offset mới = currentOffset + số lượng IDs vừa lấy ra
        int newOffset = currentOffset + pagedSeriesIds.size();
        redisTemplate.opsForValue().set(redisOffsetKey, String.valueOf(newOffset), RECOMMENDATION_POOL_TTL);

        // 5. Query PostgreSQL lấy thông tin SeriesCard
        List<SeriesCardResponseDto> fetchedCards = seriesRepository.findSeriesCardsByIds(
                new HashSet<>(pagedSeriesIds), SeriesStatus.PUBLISHED
        );

        Map<String, SeriesCardResponseDto> cardMap = fetchedCards.stream()
                .collect(Collectors.toMap(SeriesCardResponseDto::getSeriesId, c -> c, (c1, c2) -> c1));

        // Bảo toàn đúng thứ tự sắp xếp từ Redis Pool
        List<SeriesCardResponseDto> resultCards = new ArrayList<>();
        for (String id : pagedSeriesIds) {
            SeriesCardResponseDto card = cardMap.get(id);
            if (card != null) {
                resultCards.add(card);
            }
        }

        // 6. Bắn sự kiện Async sang Kafka cho ImpressionWorker
        if (!"guest_user".equals(userIdStr) && !"anonymous".equals(userIdStr)) {
            sendHomeImpressionsAsync(userIdStr, pagedSeriesIds);
            saveToAlreadyWatchedPool(userIdStr, pagedSeriesIds);
        }

        return resultCards;
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
                    pythonApi + AI_SERVICE_RANK_URL,
                    HttpMethod.POST,
                    entity,
                    new ParameterizedTypeReference<>() {
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

    /**
     * Chuyển đổi danh sách ID sang danh sách DTO theo đúng thứ tự
     */
    private List<SeriesCardResponseDto> mapIdsToDtos(List<String> ids, Map<String, SeriesCardResponseDto> cardMap) {
        if (ids == null || ids.isEmpty()) return Collections.emptyList();
        List<SeriesCardResponseDto> result = new ArrayList<>();
        for (String id : ids) {
            SeriesCardResponseDto card = cardMap.get(id);
            if (card != null) {
                result.add(card);
            }
        }
        return result;
    }

    /**
     * Khởi tạo Pool Đề xuất theo thuật toán 8 Kênh + AI LightGBM Scoring + Interleave 3:1
     */
    private void initializeRecommendationPool(String accountId, String sessionId, String pageType, String redisPoolKey) {
        log.info("[RecPool Init] Bắt đầu khởi tạo pool cho Account: {}, Session: {}, Type: {}", accountId, sessionId, pageType);

        // B1: Lấy 2 IDs từ mỗi kênh trong 8 Kênh hệ thống (Tổng tối đa 16 IDs)
        List<String> channel16Ids = fetch16IdsFrom8Channels(accountId);

        // B2: Lấy 5 series gần nhất người dùng đã xem
        List<String> recent5Series = getRecentWatchedSeries(accountId);
        List<String> filtered50Ids = new ArrayList<>();

        // Lấy danh sách IDs đã xem gần đây trong vòng 1 tiếng từ alreadyWatchPool
        Set<String> alreadyWatchedPoolIds = getAlreadyWatchedPoolIds(accountId);

        // Lấy Ids của Global và Kênh Account Sub
        Set<String> globalIds = new HashSet<>(seriesChannelService.getAllGlobalIds());
        Set<String> accountSubIds = new HashSet<>(seriesChannelService.getAllSubscribedCreatorsSeriesIds(accountId));
        Set<String> blacklist = new HashSet<>(channel16Ids);
        blacklist.addAll(globalIds);
        blacklist.addAll(accountSubIds);
        blacklist.addAll(alreadyWatchedPoolIds);

        // TH1: Người dùng CHƯA XEM bất kỳ series nào
        if (recent5Series.isEmpty()) {
            List<String> onboardingIds = seriesChannelService.getOnboardingPreferencesSeriesIds(accountId, blacklist, 50);
            if (onboardingIds != null && !onboardingIds.isEmpty()) {
                filtered50Ids.addAll(onboardingIds);
            }
        } else {
            // TH2: Người dùng ĐÃ XEM ít nhất 1 series -> Lấy tối đa 10 series tương tự cho mỗi series
            Set<String> similar50IdsSet = new LinkedHashSet<>();
            for (String seriesId : recent5Series) {
                similar50IdsSet.addAll(getSimilarSeriesIds(seriesId));
            }
            List<String> similar50Ids = new ArrayList<>(similar50IdsSet);

            // Lọc dữ liệu theo từng Trang (HOME / DETAIL)
            if ("HOME".equalsIgnoreCase(pageType)) {
                for (String id : similar50Ids) {
                    if (!blacklist.contains(id)) {
                        filtered50Ids.add(id);
                    }
                }
            } else {
                Set<String> channel16Set = new HashSet<>(channel16Ids);
                for (String id : similar50Ids) {
                    if (!channel16Set.contains(id) && !alreadyWatchedPoolIds.contains(id)) {
                        filtered50Ids.add(id);
                    }
                }
            }

            blacklist.addAll(filtered50Ids);

            // Đã xem series nhưng danh sách tương tự chưa đủ 50 -> Bổ sung thêm
            if (filtered50Ids.size() < 50) {
                int needed = 50 - filtered50Ids.size();
                UserStaticFeature staticFeature = userFeatureService.getUserStaticFeatureByAccountId(accountId);
                boolean isOlderThan1Hour = false;

                if (staticFeature != null && staticFeature.getCreatedAt() != null) {
                    Instant createdAt = staticFeature.getCreatedAt().atZone(ZoneId.systemDefault()).toInstant();
                    isOlderThan1Hour = createdAt.isBefore(Instant.now().minus(1, ChronoUnit.HOURS));
                }

                List<String> additionalIds;
                if (isOlderThan1Hour) {
                    // Tạo tài khoản > 1 tiếng -> Ưu tiên lấy theo dữ liệu động
                    additionalIds = seriesChannelService.getDynamicPreferencesSeriesIds(accountId, blacklist, needed);

                    if (additionalIds.isEmpty()){
                        additionalIds = seriesChannelService.getOnboardingPreferencesSeriesIds(accountId, blacklist, needed);
                    }
                } else {
                    // Tạo tài khoản <= 1 tiếng -> Lấy theo dữ liệu tĩnh
                    additionalIds = seriesChannelService.getOnboardingPreferencesSeriesIds(accountId, blacklist, needed);
                }

                if (additionalIds != null && !additionalIds.isEmpty()) {
                    filtered50Ids.addAll(additionalIds);
                }
            }
        }

        // B5: Gom tất cả candidates (filtered50Ids + channel16Ids) gửi sang AI chấm điểm
        Set<String> allCandidates = new LinkedHashSet<>();
        allCandidates.addAll(filtered50Ids);
        allCandidates.addAll(channel16Ids);

        List<RankResultItem> rankedItems = rankSeries(accountId, new ArrayList<>(allCandidates));

        // Tạo Map tra cứu điểm AI (Mặc định score = 0 nếu AI fallback)
        Map<String, Double> scoreMap = new HashMap<>();
        if (rankedItems != null && !rankedItems.isEmpty()) {
            for (RankResultItem item : rankedItems) {
                scoreMap.put(item.getSeriesId(), item.getScore());
            }
        }

        // Sắp xếp nhóm 50 IDs và nhóm 16 IDs giảm dần theo điểm AI
        filtered50Ids.sort((a, b) -> Double.compare(scoreMap.getOrDefault(b, 0.0), scoreMap.getOrDefault(a, 0.0)));
        channel16Ids.sort((a, b) -> Double.compare(scoreMap.getOrDefault(b, 0.0), scoreMap.getOrDefault(a, 0.0)));

        // B6: Xếp xen kẽ 3 IDs thuộc bộ 50 thì đi với 1 ID thuộc bộ 16
        List<String> finalOrderedPool = interleaveLists(filtered50Ids, channel16Ids, 3, 1);

        // B7: Lưu toàn bộ danh sách đã xếp vào Redis List Pool
        if (!finalOrderedPool.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(redisPoolKey, finalOrderedPool);
            redisTemplate.expire(redisPoolKey, RECOMMENDATION_POOL_TTL);
            log.info("[RecPool Init] Khởi tạo thành công Pool với {} series cho Session {}", finalOrderedPool.size(), sessionId);
        }
    }

    /**
     * Helper lấy 2 IDs từ 8 Kênh hệ thống (Parallel Execution)
     */
    private List<String> fetch16IdsFrom8Channels(String accountId) {
        CompletableFuture<List<String>> f1 = CompletableFuture.supplyAsync(() -> seriesChannelService.getPromotedSeriesIds(accountId, 2));
        CompletableFuture<List<String>> f2 = CompletableFuture.supplyAsync(() -> seriesChannelService.getTrendingSeriesIds(accountId, 2));
        CompletableFuture<List<String>> f3 = CompletableFuture.supplyAsync(() -> seriesChannelService.getNewReleasesSeriesIds(accountId, 2));
        CompletableFuture<List<String>> f4 = CompletableFuture.supplyAsync(() -> seriesChannelService.getRecentlyUpdatedSeriesIds(accountId, 2));
        CompletableFuture<List<String>> f5 = CompletableFuture.supplyAsync(() -> seriesChannelService.getLatestCommunityChoiceSeriesIds(accountId, 2));
        CompletableFuture<List<String>> f6 = CompletableFuture.supplyAsync(() -> seriesChannelService.getCommunityChoiceSeriesIds(accountId, 2));
        CompletableFuture<List<String>> f7 = CompletableFuture.supplyAsync(() -> seriesChannelService.getRandomCategorySeriesIds(accountId, 2));
        CompletableFuture<List<String>> f8 = CompletableFuture.supplyAsync(() -> {
            Set<String> globalIds = seriesChannelService.getAllGlobalIds();
            return seriesChannelService.getSubscribedCreatorsSeriesIds(accountId, globalIds, 2);
        });

        CompletableFuture.allOf(f1, f2, f3, f4, f5, f6, f7, f8).join();

        Set<String> channel16Set = new LinkedHashSet<>();
        try {
            channel16Set.addAll(f1.get());
            channel16Set.addAll(f2.get());
            channel16Set.addAll(f3.get());
            channel16Set.addAll(f4.get());
            channel16Set.addAll(f5.get());
            channel16Set.addAll(f6.get());
            channel16Set.addAll(f7.get());
            channel16Set.addAll(f8.get());
        } catch (Exception e) {
            log.error("[RecPool Error] Lỗi khi lấy 16 IDs từ 8 kênh: {}", e.getMessage());
        }

        return new ArrayList<>(channel16Set);
    }

    /**
     * Helper trộn xen kẽ ratioA phần tử nhóm A và ratioB phần tử nhóm B
     */
    private List<String> interleaveLists(List<String> listA, List<String> listB, int ratioA, int ratioB) {
        List<String> result = new ArrayList<>();
        Set<String> addedSet = new HashSet<>();

        int i = 0, j = 0;
        int sizeA = listA.size();
        int sizeB = listB.size();

        while (i < sizeA || j < sizeB) {
            // Lấy tối đa ratioA từ List A
            for (int k = 0; k < ratioA && i < sizeA; k++) {
                String id = listA.get(i++);
                if (addedSet.add(id)) {
                    result.add(id);
                }
            }
            // Lấy tối đa ratioB từ List B
            for (int k = 0; k < ratioB && j < sizeB; k++) {
                String id = listB.get(j++);
                if (addedSet.add(id)) {
                    result.add(id);
                }
            }
        }
        return result;
    }

    private static final String KAFKA_TOPIC_HOME_IMPRESSION = "home-impression-log-topic";

    /**
     * Gửi danh sách ID để xử lý tăng thông số
     */
    @Async("kafkaExecutor")
    public void sendHomeImpressionsAsync(String accountId, List<String> seriesIds) {
        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("accountId", accountId);
            payload.put("seriesIds", seriesIds);
            payload.put("timestamp", System.currentTimeMillis());

            String jsonPayload = objectMapper.writeValueAsString(payload);
            kafkaTemplate.send(KAFKA_TOPIC_HOME_IMPRESSION, accountId, jsonPayload);
            log.info("[Kafka Impression] Đã phát log impression cho accountId: {} với {} series", accountId, seriesIds.size());
        } catch (Exception e) {
            log.error("[Kafka Impression Error] Lỗi khi phát log impression cho accountId {}: {}", accountId, e.getMessage(), e);
        }
    }

    /**
     * Lưu danh sách các Series IDs vừa lấy hiển thị vào Redis Set (TTL 1 Giờ)
     */
    private void saveToAlreadyWatchedPool(String accountId, List<String> seriesIds) {
        if (accountId == null || accountId.trim().isEmpty() || seriesIds == null || seriesIds.isEmpty()) {
            return;
        }
        String key = RECOMMENDATION_ALREADY_WATCHED_PREFIX + accountId;
        try {
            redisTemplate.opsForSet().add(key, seriesIds.toArray(new String[0]));
            redisTemplate.expire(key, ALREADY_WATCHED_POOL_TTL);
        } catch (Exception e) {
            log.error("[AlreadyWatchedPool Error] Lỗi khi lưu Redis cho accountId {}: {}", accountId, e.getMessage());
        }
    }

    /**
     * Lấy tập hợp các Series IDs đã hiển thị gần đây (trong 1 Giờ qua) từ Redis Set
     */
    private Set<String> getAlreadyWatchedPoolIds(String accountId) {
        if (accountId == null || accountId.trim().isEmpty() || "guest_user".equals(accountId) || "anonymous".equals(accountId)) {
            return Collections.emptySet();
        }
        String key = RECOMMENDATION_ALREADY_WATCHED_PREFIX + accountId;
        try {
            Set<String> members = redisTemplate.opsForSet().members(key);
            return members != null ? members : Collections.emptySet();
        } catch (Exception e) {
            log.error("[AlreadyWatchedPool Error] Lỗi khi đọc Redis cho accountId {}: {}", accountId, e.getMessage());
            return Collections.emptySet();
        }
    }
}
