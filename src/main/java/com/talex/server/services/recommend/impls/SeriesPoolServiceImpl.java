package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.recommend.response.SeriesChannelConfigRes;
import com.talex.server.services.recommend.SeriesChannelConfigService;
import com.talex.server.services.recommend.SeriesChannelService;
import com.talex.server.services.recommend.SeriesPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesPoolServiceImpl implements SeriesPoolService {
    private final SeriesChannelService seriesChannelService;
    private final SeriesChannelConfigService configService;

    /**
     * Trật tự thực thi:
     * 1. Promoted Channel
     * 2. Latest Community Choice Channel
     * 3. Community Choice Channel
     * 4. New Releases Channel
     * 5. Recently Updated Channel
     * 6. Random Category Channel
     */
    @Override
    public void rebuildAllGlobalPools() {
        // Dùng LinkedHashSet để giữ thứ tự ưu tiên và đảm bảo không chứa trùng lặp
        Set<String> cumulativeBlacklist = new LinkedHashSet<>();

        try {
            SeriesChannelConfigRes config = configService.getConfig();

            // 1. Promoted Channel (Kênh Quảng Cáo - Ưu tiên hàng đầu)
            List<String> promotedIds = seriesChannelService.refreshPromotedPool(config.getPromotedPoolNumber());
            cumulativeBlacklist.addAll(promotedIds);

            // 2. New Releases Channel (Mới phát hành)
            List<String> newReleasesIds = seriesChannelService.refreshNewReleasesPool(
                    new ArrayList<>(cumulativeBlacklist), config.getNewReleasedPoolNumber()
            );
            cumulativeBlacklist.addAll(newReleasesIds);

            // 2. Trending Channel (Kênh Xu Hướng)
            List<String> trendingIds = seriesChannelService.refreshTrendingPool(
                    new ArrayList<>(cumulativeBlacklist), config.getTrendingPoolNumber()
            );
            cumulativeBlacklist.addAll(trendingIds);

            // 3. Latest Community Choice Channel (Cộng đồng bình chọn)
            List<String> latestCommunityIds = seriesChannelService.refreshLatestCommunityChoicePool(
                    new ArrayList<>(cumulativeBlacklist), config.getLatestCommunityChoicePoolNumber()
            );
            cumulativeBlacklist.addAll(latestCommunityIds);

            // 4. Community Choice Channel (Cộng đồng bình chọn All-time)
            List<String> communityChoiceIds = seriesChannelService.refreshCommunityChoicePool(
                    new ArrayList<>(cumulativeBlacklist), config.getCommunityChoicePoolNumber()
            );
            cumulativeBlacklist.addAll(communityChoiceIds);

            // 6. Recently Updated Channel (Mới cập nhật tập mới)
            List<String> recentlyUpdatedIds = seriesChannelService.refreshRecentlyUpdatedPool(
                    new ArrayList<>(cumulativeBlacklist), config.getRecentlyUpdatedPoolNumber()
            );
            cumulativeBlacklist.addAll(recentlyUpdatedIds);

            // 7. Random Category Channel (Thể loại ngẫu nhiên - Mặc định 3 series/category, tối đa 50 series)
            List<String> randomCategoryIds = seriesChannelService.refreshRandomCategoryPool(
                    new ArrayList<>(cumulativeBlacklist), config.getNumberPerCategory(), config.getRandomCategoryPoolNumber()
            );
            cumulativeBlacklist.addAll(randomCategoryIds);

            // 8. Cập nhật Global Candidate IDs vào Redis 1 lần duy nhất sau khi hoàn tất
            seriesChannelService.updateGlobalIds(cumulativeBlacklist);

        } catch (Exception e) {
            log.error("[SeriesPoolService] LỖI TRONG QUÁ TRÌNH TÁI TẠO POOL CRON JOB: {}", e.getMessage(), e);
        }
    }
}
