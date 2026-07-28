package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.mongo.UserDynamicFeature;
import com.talex.server.dtos.mongo.UserStaticFeature;
import com.talex.server.enums.ImpressionStatus;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.series.CategoryStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.repositories.series.SeriesLogRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.mongo.IUserFeatureService;
import com.talex.server.services.recommend.SeriesChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesChannelServiceImpl implements SeriesChannelService {
    private final IUserFeatureService userFeatureService;
    private final SeriesRepository seriesRepository;
    private final SeriesLogRepository seriesLogRepository;
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final StringRedisTemplate redisTemplate;

    @Value("${app.recommendation.new-releases.max-impression}")
    private Long maxImpressionThreshold;

    // Redis Keys - Promoted
    private static final String REDIS_KEY_PROMOTED_POOL = "pool:promoted";
    private static final String REDIS_KEY_PROMOTED_OFFSET_PREFIX = "offset:promoted:";

    // Redis Keys - New Releases
    private static final String REDIS_KEY_NEW_RELEASES_POOL = "pool:new_releases";
    private static final String REDIS_KEY_NEW_RELEASES_OFFSET_PREFIX = "offset:new_releases:";

    // Redis Keys - Recently Updated
    private static final String REDIS_KEY_RECENTLY_UPDATED_POOL = "pool:recently_updated";
    private static final String REDIS_KEY_RECENTLY_UPDATED_OFFSET_PREFIX = "offset:recently_updated:";

    // Redis Keys - Latest Community Choice
    private static final String REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL = "pool:latest_community_choice";
    private static final String REDIS_KEY_LATEST_COMMUNITY_CHOICE_OFFSET_PREFIX = "offset:latest_community_choice:";

    // Redis Keys - Community Choice
    private static final String REDIS_KEY_COMMUNITY_CHOICE_POOL = "pool:community_choice";
    private static final String REDIS_KEY_COMMUNITY_CHOICE_OFFSET_PREFIX = "offset:community_choice:";

    // Redis Keys - Random Category Channel
    private static final String REDIS_KEY_RANDOM_CATEGORY_POOL = "pool:random_category";
    private static final String REDIS_KEY_RANDOM_CATEGORY_OFFSET_PREFIX = "offset:random_category:";

    // Redis Keys - Subscribed Creator Channel
    private static final String REDIS_KEY_SUBSCRIBED_CREATORS_POOL_PREFIX = "pool:subscribed_creators:";
    private static final String REDIS_KEY_SUBSCRIBED_CREATORS_OFFSET_PREFIX = "offset:subscribed_creators:";

    // Redis Keys - Trending Channel
    private static final String REDIS_KEY_TRENDING_POOL = "pool:trending";
    private static final String REDIS_KEY_TRENDING_OFFSET_PREFIX = "offset:trending:";

    // Global IDs Key
    private static final String REDIS_KEY_GLOBAL_IDS = "recommendation:global_ids";
    private static final Duration POOL_TTL = Duration.ofDays(1);

    // =========================================================================
    // 1. PROMOTED CHANNEL
    // =========================================================================

    @Override
    public List<String> getPromotedSeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_PROMOTED_POOL);

        // Fallback: Nếu Pool trống, kích hoạt nạp từ DB
        if (poolSize == null || poolSize == 0) {
            List<String> refreshed = refreshPromotedPool(limit);
            if (refreshed.isEmpty()) return Collections.emptyList();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_PROMOTED_POOL,
                REDIS_KEY_PROMOTED_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshPromotedPool(int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // 1. Lấy danh sách IDs cũ từ Redis Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_PROMOTED_POOL, 0, -1);
        if (oldIds == null) {
            oldIds = Collections.emptyList();
        }

        // 2. Truy vấn DB lấy các Candidate Series IDs mới
        List<String> newFetchedIds = campaignSeriesRepository.findActivePromotedSeriesIds(
                CampaignStatus.RUNNING,
                oldIds,
                oldIds.isEmpty(),
                pageable
        );

        List<String> mergedList = mergeOldAndNewIds(oldIds, newFetchedIds, limit);

        // 3. Ghi đè lại vào Redis RAM
        redisTemplate.delete(REDIS_KEY_PROMOTED_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_PROMOTED_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_PROMOTED_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // 2. NEW RELEASES CHANNEL
    // =========================================================================

    @Override
    public List<String> getNewReleasesSeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_NEW_RELEASES_POOL);

        // Fallback: Nếu Pool trống, tự động kích hoạt refresh không dùng blacklist
        if (poolSize == null || poolSize == 0) {
            List<String> refreshed = refreshNewReleasesPool(Collections.emptyList(), limit);
            if (refreshed.isEmpty()) return Collections.emptyList();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_NEW_RELEASES_POOL,
                REDIS_KEY_NEW_RELEASES_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshNewReleasesPool(List<String> blacklistIds, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // 1. Lấy danh sách IDs cũ từ Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_NEW_RELEASES_POOL, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist: Bao gồm blacklistIds từ kênh cấp trên + oldIds của chính nó để tránh trùng trong query
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Query PostgreSQL lấy các Series mới phát hành có totalImpression <= maxImpressionThreshold
        List<String> newFetchedIds = seriesRepository.findCandidateNewReleasesSeriesIds(
                SeriesStatus.PUBLISHED,
                maxImpressionThreshold,
                combinedBlacklist,
                isBlacklistEmpty,
                ImpressionStatus.ON_GOING,
                pageable
        );

        // 4. Áp dụng logic ghép: giữ vị trí cũ + chèn danh sách mới
        List<String> mergedList = mergeOldAndNewIds(oldIds, newFetchedIds, limit);

        // 5. Cập nhật Redis Pool
        redisTemplate.delete(REDIS_KEY_NEW_RELEASES_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_NEW_RELEASES_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_NEW_RELEASES_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // 3. RECENTLY UPDATED CHANNEL (KÊNH MỚI CẬP NHẬT)
    // =========================================================================

    @Override
    public List<String> getRecentlyUpdatedSeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_RECENTLY_UPDATED_POOL);

        // Fallback: Nếu Pool trống, tự động kích hoạt refresh không dùng blacklist
        if (poolSize == null || poolSize == 0) {
            log.warn("[RecentlyUpdatedChannel] Redis pool '{}' bị trống! Đang kích hoạt Fallback...", REDIS_KEY_RECENTLY_UPDATED_POOL);
            List<String> refreshed = refreshRecentlyUpdatedPool(Collections.emptyList(), limit);
            if (refreshed.isEmpty()) return Collections.emptyList();
            poolSize = (long) refreshed.size();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_RECENTLY_UPDATED_POOL,
                REDIS_KEY_RECENTLY_UPDATED_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshRecentlyUpdatedPool(List<String> blacklistIds, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // 1. Lấy danh sách IDs cũ từ Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_RECENTLY_UPDATED_POOL, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist: Bao gồm blacklistIds truyền từ kênh cấp trên + oldIds của chính nó
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Query PostgreSQL lấy các Series công khai xếp giảm dần theo thời gian releasedUpdateTime DESC
        List<String> newFetchedIds = seriesRepository.findCandidateRecentlyUpdatedSeriesIds(
                SeriesStatus.PUBLISHED,
                combinedBlacklist,
                isBlacklistEmpty,
                pageable
        );

        // 4. Áp dụng logic ghép: giữ vị trí cũ + chèn danh sách mới
        List<String> mergedList = mergeOldAndNewIds(oldIds, newFetchedIds, limit);

        // 5. Cập nhật Redis Pool
        redisTemplate.delete(REDIS_KEY_RECENTLY_UPDATED_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_RECENTLY_UPDATED_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_RECENTLY_UPDATED_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // 4. COMMUNITY CHOICE CHANNEL (KÊNH THEO GIỜ NEAR-REALTIME)
    // =========================================================================

    @Override
    public List<String> getLatestCommunityChoiceSeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL);

        // Fallback: Nếu Pool trống, tự động kích hoạt refresh không dùng blacklist
        if (poolSize == null || poolSize == 0) {
            List<String> refreshed = refreshLatestCommunityChoicePool(Collections.emptyList(), limit);
            if (refreshed.isEmpty()) return Collections.emptyList();
            poolSize = (long) refreshed.size();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL,
                REDIS_KEY_LATEST_COMMUNITY_CHOICE_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshLatestCommunityChoicePool(List<String> blacklistIds, int limit) {
        // 1. Lấy danh sách IDs cũ từ Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Làm tròn mốc thời gian về đầu giờ hiện tại (VD: 16:08 -> 16:00:00)
        LocalDateTime currentHourTruncated = LocalDateTime.now().truncatedTo(ChronoUnit.HOURS);

        // 4. Query DB sắp xếp ưu tiên hourBucket gần nhất trước, sau đó tới watchTime -> likes -> views
        List<String> newFetchedIds = seriesLogRepository.findCandidateTrendingSeriesIds(
                SeriesStatus.PUBLISHED.name(),
                currentHourTruncated,
                combinedBlacklist,
                isBlacklistEmpty,
                limit
        );

        // 5. Áp dụng logic ghép: giữ vị trí cũ + chèn danh sách mới
        List<String> mergedList = mergeOldAndNewIds(oldIds, newFetchedIds, limit);

        // 6. Cập nhật Redis Pool
        redisTemplate.delete(REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_LATEST_COMMUNITY_CHOICE_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // 5. COMMUNITY CHOICE CHANNEL (CỘNG ĐỒNG BÌNH CHỌN ALL-TIME)
    // =========================================================================

    @Override
    public List<String> getCommunityChoiceSeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_COMMUNITY_CHOICE_POOL);

        // Fallback: Nếu Pool trống, tự động kích hoạt refresh không dùng blacklist
        if (poolSize == null || poolSize == 0) {
            log.warn("[CommunityChoiceChannel] Redis pool '{}' bị trống! Đang kích hoạt Fallback...", REDIS_KEY_COMMUNITY_CHOICE_POOL);
            List<String> refreshed = refreshCommunityChoicePool(Collections.emptyList(), limit);
            if (refreshed.isEmpty()) return Collections.emptyList();
            poolSize = (long) refreshed.size();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_COMMUNITY_CHOICE_POOL,
                REDIS_KEY_COMMUNITY_CHOICE_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshCommunityChoicePool(List<String> blacklistIds, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // 1. Lấy danh sách IDs cũ từ Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_COMMUNITY_CHOICE_POOL, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist: Dùng blacklist cấp trên + oldIds của chính nó
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Query PostgreSQL lấy Series công khai xếp giảm dần theo chỉ số All-time (watchTime -> likes -> views -> comments -> shares -> bookmarks)
        List<String> newFetchedIds = seriesRepository.findCandidateCommunityChoiceSeriesIds(
                SeriesStatus.PUBLISHED,
                combinedBlacklist,
                isBlacklistEmpty,
                pageable
        );

        // 4. Áp dụng logic ghép: giữ vị trí cũ + chèn danh sách mới
        List<String> mergedList = mergeOldAndNewIds(oldIds, newFetchedIds, limit);

        // 5. Cập nhật Redis Pool
        redisTemplate.delete(REDIS_KEY_COMMUNITY_CHOICE_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_COMMUNITY_CHOICE_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_COMMUNITY_CHOICE_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // 6. RANDOM CATEGORY CHANNEL (KÊNH THỂ LOẠI NGẪU NHIÊN)
    // =========================================================================

    @Override
    public List<String> getRandomCategorySeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_RANDOM_CATEGORY_POOL);

        // Fallback: Nếu Pool trống, kích hoạt nạp tự động với mặc định 3 series / category
        if (poolSize == null || poolSize == 0) {
            log.warn("[RandomCategoryChannel] Redis pool '{}' bị trống! Đang kích hoạt Fallback...", REDIS_KEY_RANDOM_CATEGORY_POOL);
            List<String> refreshed = refreshRandomCategoryPool(Collections.emptyList(), 3, 20);
            if (refreshed.isEmpty()) return Collections.emptyList();
            poolSize = (long) refreshed.size();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_RANDOM_CATEGORY_POOL,
                REDIS_KEY_RANDOM_CATEGORY_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshRandomCategoryPool(List<String> blacklistIds, int limitPerCategory, int totalLimit) {
        // 1. Lấy danh sách IDs cũ từ Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_RANDOM_CATEGORY_POOL, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist: Dùng blacklist các kênh trước + oldIds của chính nó
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Đúng 1 Query duy nhất lấy Top N Series hay nhất của mỗi Category bằng Window Function
        List<String> candidateIds = seriesRepository.findTopSeriesPerCategory(
                SeriesStatus.PUBLISHED.name(),
                CategoryStatus.ACTIVE.name(),
                combinedBlacklist,
                isBlacklistEmpty,
                limitPerCategory
        );

        // 4. Trộn ngẫu nhiên danh sách Series thu thập từ các thể loại để tạo tính "ngẫu nhiên"
        List<String> shuffledNewIds = new ArrayList<>(candidateIds);
        Collections.shuffle(shuffledNewIds);

        // 5. Áp dụng logic ghép: giữ vị trí cũ + chèn danh sách mới thu được
        List<String> mergedList = mergeOldAndNewIds(oldIds, shuffledNewIds, totalLimit);

        // 6. Cập nhật Redis Pool
        redisTemplate.delete(REDIS_KEY_RANDOM_CATEGORY_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_RANDOM_CATEGORY_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_RANDOM_CATEGORY_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // 7. SUBSCRIBED CREATORS CHANNEL (KÊNH TÁC GIẢ ĐÃ ĐĂNG KÝ)
    // =========================================================================

    @Override
    public List<String> getSubscribedCreatorsSeriesIds(String accountId, Set<String> blacklistIds, int limit) {
        if (accountId == null || accountId.trim().isEmpty() || limit <= 0) {
            return Collections.emptyList();
        }

        String poolKey = REDIS_KEY_SUBSCRIBED_CREATORS_POOL_PREFIX + accountId;
        Long poolSize = redisTemplate.opsForList().size(poolKey);

        // Fallback: Nếu Pool cá nhân bị trống, kích hoạt làm mới tự động với mặc định 3 series / creator
        if (poolSize == null || poolSize == 0) {
            List<String> refreshed = refreshSubscribedCreatorsPool(accountId, blacklistIds, 3, 20);
            if (refreshed.isEmpty()) return Collections.emptyList();
            poolSize = (long) refreshed.size();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                poolKey,
                REDIS_KEY_SUBSCRIBED_CREATORS_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshSubscribedCreatorsPool(String accountId, Set<String> blacklistIds, int limitPerCreator, int totalLimit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String poolKey = REDIS_KEY_SUBSCRIBED_CREATORS_POOL_PREFIX + accountId;

        // 1. Lấy danh sách IDs cũ từ Redis Pool cá nhân
        List<String> oldIds = redisTemplate.opsForList().range(poolKey, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist: Dùng blacklist các kênh trước + oldIds của chính user này
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Gọi 1 Query duy nhất quét PostgreSQL lấy Series từ các Creator mà user đã follow
        List<String> candidateIds = seriesRepository.findTopSeriesFromFollowedCreators(
                accountId,
                SeriesStatus.PUBLISHED.name(),
                combinedBlacklist,
                isBlacklistEmpty,
                limitPerCreator
        );

        // 4. Xáo trộn ngẫu nhiên danh sách để nội dung các tác giả đan xen nhau hấp dẫn hơn
        List<String> shuffledNewIds = new ArrayList<>(candidateIds);
        Collections.shuffle(shuffledNewIds);

        // 5. Trộn vị trí cũ và mới
        List<String> mergedList = mergeOldAndNewIds(oldIds, shuffledNewIds, totalLimit);

        // 6. Cập nhật vào Redis Pool cá nhân
        redisTemplate.delete(poolKey);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(poolKey, mergedList);
            redisTemplate.expire(poolKey, Duration.ofHours(1)); // Đặt TTL 1 hour
        }

        return mergedList;
    }

    @Override
    public List<String> getAllSubscribedCreatorsSeriesIds(String accountId) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        String redisKey = REDIS_KEY_SUBSCRIBED_CREATORS_POOL_PREFIX + accountId;
        List<String> allIds = redisTemplate.opsForList().range(redisKey, 0, -1);

        return (allIds != null) ? allIds : Collections.emptyList();
    }

    // =========================================================================
    // TRENDING CHANNEL (KÊNH XU HƯỚNG)
    // =========================================================================

    @Override
    public List<String> getTrendingSeriesIds(String accountId, int limit) {
        if (limit <= 0) return Collections.emptyList();

        Long poolSize = redisTemplate.opsForList().size(REDIS_KEY_TRENDING_POOL);

        // Fallback: Nếu Pool trống, tự động kích hoạt refresh không dùng blacklist
        if (poolSize == null || poolSize == 0) {
            log.warn("[TrendingChannel] Redis pool '{}' bị trống! Đang kích hoạt Fallback...", REDIS_KEY_TRENDING_POOL);
            List<String> refreshed = refreshTrendingPool(Collections.emptyList(), limit);
            if (refreshed.isEmpty()) return Collections.emptyList();
            poolSize = (long) refreshed.size();
        }

        return getIdsWithOffset(
                accountId, limit, poolSize,
                REDIS_KEY_TRENDING_POOL,
                REDIS_KEY_TRENDING_OFFSET_PREFIX
        );
    }

    @Override
    public List<String> refreshTrendingPool(List<String> blacklistIds, int limit) {
        Pageable pageable = PageRequest.of(0, limit);

        // 1. Lấy danh sách IDs cũ từ Pool hiện tại
        List<String> oldIds = redisTemplate.opsForList().range(REDIS_KEY_TRENDING_POOL, 0, -1);
        if (oldIds == null) oldIds = Collections.emptyList();

        // 2. Gom Blacklist
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        combinedBlacklist.addAll(oldIds);

        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Query PostgreSQL lấy các Series có impressionStatus = SUCCESS và rankingScore > 0 xếp DESC
        List<String> newFetchedIds = seriesRepository.findCandidateTrendingSeriesIds(
                SeriesStatus.PUBLISHED,
                ImpressionStatus.SUCCESS,
                combinedBlacklist,
                isBlacklistEmpty,
                pageable
        );

        // 4. Áp dụng logic ghép: giữ vị trí cũ + chèn danh sách mới
        List<String> mergedList = mergeOldAndNewIds(oldIds, newFetchedIds, limit);

        // 5. Cập nhật Redis Pool
        redisTemplate.delete(REDIS_KEY_TRENDING_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_TRENDING_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_TRENDING_POOL, POOL_TTL);
        }

        return mergedList;
    }

    // =========================================================================
    // ONBOARDING PREFERENCES CHANNEL (KÊNH SỞ THÍCH ONBOARDING)
    // =========================================================================

    @Override
    public List<String> getOnboardingPreferencesSeriesIds(String accountId, Set<String> blacklistIds, int totalLimit) {
        if (accountId == null || accountId.trim().isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Lấy dữ liệu tĩnh (Onboarding Features) từ UserFeatureService
        UserStaticFeature staticFeature = userFeatureService.getUserStaticFeatureByAccountId(accountId);
        if (staticFeature == null) {
            log.warn("[OnboardingChannel] Không tìm thấy dữ liệu tĩnh cho Account ID: {}", accountId);
            return Collections.emptyList();
        }

        List<String> genres = staticFeature.getOnboardingGenres();
        List<String> tags = staticFeature.getOnboardingTags();

        boolean hasGenres = genres != null && !genres.isEmpty();
        boolean hasTags = tags != null && !tags.isEmpty();

        // Nếu user chưa chọn thể loại lẫn tag nào lúc onboarding -> không thể đề xuất theo kênh này
        if (!hasGenres && !hasTags) {
            log.info("[OnboardingChannel] Account ID {} không có Onboarding Genres/Tags.", accountId);
            return Collections.emptyList();
        }

        // 3. Gom Blacklist
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) combinedBlacklist.addAll(blacklistIds);
        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 4. Query PostgreSQL tìm Candidate Series IDs phù hợp
        Pageable pageable = PageRequest.of(0, totalLimit);
        List<String> candidateIds = seriesRepository.findCandidateSeriesByGenresAndTags(
                SeriesStatus.PUBLISHED,
                hasGenres ? genres : Collections.emptyList(),
                hasGenres,
                hasTags ? tags : Collections.emptyList(),
                hasTags,
                combinedBlacklist,
                isBlacklistEmpty,
                pageable
        );

        // 5. Trộn ngẫu nhiên để nội dung thêm đa dạng khi người dùng xem lại
        List<String> shuffledNewIds = new ArrayList<>(candidateIds);
        Collections.shuffle(shuffledNewIds);

        return shuffledNewIds;
    }

    // =========================================================================
    // DYNAMIC PREFERENCES CHANNEL (KÊNH SỞ THÍCH ĐỘNG TƯƠNG TÁC)
    // =========================================================================

    @Override
    public List<String> getDynamicPreferencesSeriesIds(String accountId, Set<String> blacklistIds, int totalLimit) {
        if (accountId == null || accountId.trim().isEmpty() || totalLimit <= 0) {
            return Collections.emptyList();
        }

        // 1. Lấy dữ liệu đặc trưng động (Dynamic Features) từ UserFeatureService
        UserDynamicFeature dynamicFeature = userFeatureService.getUserDynamicFeatureByAccountId(accountId);
        if (dynamicFeature == null) {
            log.warn("[DynamicChannel] Không tìm thấy dữ liệu động cho Account ID: {}", accountId);
            return Collections.emptyList();
        }

        List<String> categories = dynamicFeature.getCategories();
        List<String> tags = dynamicFeature.getTags();

        boolean hasCategories = categories != null && !categories.isEmpty();
        boolean hasTags = tags != null && !tags.isEmpty();

        // Nếu user chưa có lịch sử tương tác/thời gian xem đủ tạo thành sở thích động -> bỏ qua
        if (!hasCategories && !hasTags) {
            log.info("[DynamicChannel] Account ID {} chưa có Dynamic Categories/Tags tương tác.", accountId);
            return Collections.emptyList();
        }

        // 2. Gom Blacklist
        Set<String> combinedBlacklist = new HashSet<>();
        if (blacklistIds != null) {
            combinedBlacklist.addAll(blacklistIds);
        }
        boolean isBlacklistEmpty = combinedBlacklist.isEmpty();

        // 3. Query PostgreSQL tìm Candidate Series IDs phù hợp với Categories & Tags động
        Pageable pageable = PageRequest.of(0, totalLimit);
        List<String> candidateIds = seriesRepository.findCandidateSeriesByGenresAndTags(
                SeriesStatus.PUBLISHED,
                hasCategories ? categories : Collections.emptyList(),
                hasCategories,
                hasTags ? tags : Collections.emptyList(),
                hasTags,
                combinedBlacklist,
                isBlacklistEmpty,
                pageable
        );

        // 4. Trộn ngẫu nhiên danh sách ứng viên để tăng tính đa dạng hiển thị
        List<String> shuffledNewIds = new ArrayList<>(candidateIds);
        Collections.shuffle(shuffledNewIds);

        return shuffledNewIds;
    }

    // =========================================================================
    // 7. GLOBAL IDS (Tất cả Series trong các kênh)
    // =========================================================================

    @Override
    public Set<String> getAllGlobalIds() {
        Set<String> globalIds = redisTemplate.opsForSet().members(REDIS_KEY_GLOBAL_IDS);
        return (globalIds != null) ? globalIds : Collections.emptySet();
    }

    @Override
    @Async("interactionExecutor")
    public void updateGlobalIds(Set<String> globalIds) {
        if (globalIds == null || globalIds.isEmpty()) {
            redisTemplate.delete(REDIS_KEY_GLOBAL_IDS);
            log.info("[GlobalIDs] Danh sách rỗng, đã dọn dẹp key '{}' trên Redis.", REDIS_KEY_GLOBAL_IDS);
            return;
        }

        try {
            // 1. Xóa Set cũ để chuẩn bị cập nhật dữ liệu mới nhất
            redisTemplate.delete(REDIS_KEY_GLOBAL_IDS);

            // 2. Nạp toàn bộ danh sách Candidate IDs mới vào Redis Set
            redisTemplate.opsForSet().add(REDIS_KEY_GLOBAL_IDS, globalIds.toArray(new String[0]));

            // 3. Đặt thời gian hết hạn cho Key (bằng TTL với các Pool)
            redisTemplate.expire(REDIS_KEY_GLOBAL_IDS, POOL_TTL);

            log.info("[GlobalIDs - ASYNC] Đã cập nhật thành công {} Candidate IDs vào Redis Set '{}'.",
                    globalIds.size(), REDIS_KEY_GLOBAL_IDS);

        } catch (Exception e) {
            log.error("[GlobalIDs - ASYNC ERROR] Lỗi khi ghi đè Global Candidate IDs lên Redis: {}", e.getMessage(), e);
        }
    }

    // =========================================================================
    // HELPER METHODS
    // =========================================================================

    /**
     * Hàm chung lấy dữ liệu theo Offset xoay vòng từ Redis Pool.
     */
    private List<String> getIdsWithOffset(String accountId, int limit, long poolSize, String poolKey, String offsetPrefix) {
        String offsetKey = offsetPrefix + accountId;
        String currentOffsetStr = redisTemplate.opsForValue().get(offsetKey);
        long currentOffset = (currentOffsetStr != null) ? Long.parseLong(currentOffsetStr) : 0L;

        if (currentOffset >= poolSize) {
            currentOffset = 0L;
        }

        List<String> result = new ArrayList<>();

        if (currentOffset + limit <= poolSize) {
            List<String> fetched = redisTemplate.opsForList().range(poolKey, currentOffset, currentOffset + limit - 1);
            if (fetched != null) result.addAll(fetched);
        } else {
            // Đọc đến cuối danh sách
            List<String> part1 = redisTemplate.opsForList().range(poolKey, currentOffset, poolSize - 1);
            if (part1 != null) result.addAll(part1);

            // Quay đầu lấy phần thiếu ở đầu danh sách (Circular offset)
            long remaining = limit - result.size();
            List<String> part2 = redisTemplate.opsForList().range(poolKey, 0, remaining - 1);
            if (part2 != null) result.addAll(part2);
        }

        long nextOffset = (currentOffset + limit) % poolSize;
        redisTemplate.opsForValue().set(offsetKey, String.valueOf(nextOffset));

        return result;
    }

    /**
     * Hàm hợp nhất danh sách IDs cũ và mới theo quy tắc giữ nguyên vị trí cũ.
     */
    private List<String> mergeOldAndNewIds(List<String> oldIds, List<String> newFetchedIds, int limit) {
        List<String> mergedList = new ArrayList<>();

        if (newFetchedIds.size() >= limit) {
            mergedList.addAll(newFetchedIds.subList(0, limit));
        } else {
            int remainder = limit - newFetchedIds.size();
            int retainCount = Math.min(remainder, oldIds.size());

            if (retainCount > 0) {
                mergedList.addAll(oldIds.subList(0, retainCount));
            }
            mergedList.addAll(newFetchedIds);
        }

        return mergedList;
    }
}
