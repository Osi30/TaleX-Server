package com.talex.server.services.recommend.impls;

import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.services.recommend.SeriesChannelService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesChannelServiceImpl implements SeriesChannelService {
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY_PROMOTED_POOL = "pool:promoted";
    private static final Duration POOL_TTL = Duration.ofDays(1);

    @Override
    public List<String> getPromotedSeriesIds(List<String> blacklistIds, int limit) {
        if (limit <= 0) return Collections.emptyList();

        // 1. Đọc dữ liệu từ đệm Redis Pool
        List<String> cachedIds = redisTemplate.opsForList().range(REDIS_KEY_PROMOTED_POOL, 0, limit - 1);

        // 2. Phòng ngừa sự cố (Fallback): Nếu Pool trên Redis bị trống, lập tức nạp lại từ DB
        if (cachedIds == null || cachedIds.isEmpty()) {
            log.warn("[PromotedChannel] Redis pool '{}' bị trống! Đang kích hoạt cơ chế Fallback truy vấn PostgreSQL...", REDIS_KEY_PROMOTED_POOL);
            return refreshPromotedPool(limit);
        }

        // 3. Lọc bỏ các IDs dính blacklist truyền vào (nếu có)
//        if (blacklistIds != null && !blacklistIds.isEmpty()) {
//            Set<String> blacklistSet = new HashSet<>(blacklistIds);
//            cachedIds = cachedIds.stream()
//                    .filter(id -> !blacklistSet.contains(id))
//                    .toList();
//        }

        return cachedIds;
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

        // 3. Chèn newFetchedIds vào vị trí đầu tiên, giữ nguyên thứ tự các oldIds
        Set<String> mergedSet = new LinkedHashSet<>(newFetchedIds);
        mergedSet.addAll(oldIds);

        List<String> mergedList = new ArrayList<>(mergedSet);
        if (mergedList.size() > limit) {
            mergedList = mergedList.subList(0, limit);
        }

        // 4. Ghi đè lại vào Redis RAM
        redisTemplate.delete(REDIS_KEY_PROMOTED_POOL);
        if (!mergedList.isEmpty()) {
            redisTemplate.opsForList().rightPushAll(REDIS_KEY_PROMOTED_POOL, mergedList);
            redisTemplate.expire(REDIS_KEY_PROMOTED_POOL, POOL_TTL);
        }

        log.info("[PromotedChannel] Đã làm mới Redis Pool '{}' với {} series IDs.", REDIS_KEY_PROMOTED_POOL, mergedList.size());
        return mergedList;
    }
}
