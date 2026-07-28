package com.talex.server.schedulers;

import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.mongo.ISeriesFeatureService;
import com.talex.server.services.mongo.IUserFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoFeatureSyncScheduler {
    private final IUserFeatureService userFeatureService;
    private final ISeriesFeatureService seriesFeatureService;
    private final SeriesRepository seriesRepository;
    private final AccountRepository accountRepository;

    /// Đồng bộ tự động thói quen người dùng
    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicFeatureSync() {
        try {
            userFeatureService.syncUserDynamicFeatures();
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi kích hoạt tiến trình đồng bộ tự động thói quen người dùng (Dynamic Feature): ", e);
        }
    }

    /// Đồng bộ tự động thói quen người dùng
    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicPreferencesSync() {
        try {
            userFeatureService.syncUserDynamicPreferences();
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi kích hoạt tiến trình đồng bộ tự động thói quen người dùng (Dynamic Pref): ", e);
        }
    }

    /// Đồng bộ tự động đặc điểm của Series
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleHourlySeriesStatsSync() {
        try {
            seriesFeatureService.syncAllSeriesFeatures();
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi kích hoạt tiến trình đồng bộ tự động series (Series Feature): ", e);
        }
    }

    /// Dọn dẹp cho series mỗi ngày (dữ liệu 24h và 7d)
    @Scheduled(cron = "0 0 1 * * ?")
    public void runDailyInactiveSeriesReset() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold24h = now.minusHours(24);
        LocalDateTime threshold7d = now.minusDays(7);

        try {
            // 1. Xử lý reset stats 24h
            List<String> needs24hReset = seriesRepository.findSeriesIdsFor24hReset(threshold24h);
            if (!needs24hReset.isEmpty()) {
                seriesFeatureService.resetInactiveSeriesStatsInMongo(needs24hReset, true, false);
                seriesRepository.markAs24hSynced(needs24hReset);
            }

            // 2. Xử lý reset stats 7d
            List<String> needs7dReset = seriesRepository.findSeriesIdsFor7dReset(threshold7d);
            if (!needs7dReset.isEmpty()) {
                seriesFeatureService.resetInactiveSeriesStatsInMongo(needs7dReset, false, true);
                seriesRepository.markAs7dSynced(needs7dReset);
            }

        } catch (Exception e) {
            log.error("[Daily Reset Cron] Gặp lỗi nghiêm trọng khi dọn dẹp cho series: ", e);
        }
    }

    /// Dọn dẹp cho account mỗi ngày (dữ liệu 24h và 7d)
    @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processExpiredFeaturesCleanUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold24h = now.minusHours(24);
        LocalDateTime threshold7d = now.minusDays(7);

        try {
            // --- XỬ LÝ 24 GIỜ ---
            List<UUID> expired24hAccountIds = accountRepository.findExpired24hAccountIds(threshold24h);
            if (!expired24hAccountIds.isEmpty()) {
                // Convert List<UUID> sang List<String> phù hợp với _id của MongoDB Document
                List<String> mongoIds = expired24hAccountIds.stream().map(UUID::toString).toList();
                userFeatureService.cleanUp24hFeatures(mongoIds);
                accountRepository.updateIs24hByAccountIds(expired24hAccountIds);
            }

            // --- XỬ LÝ 7 NGÀY ---
            List<UUID> expired7dAccountIds = accountRepository.findExpired7dAccountIds(threshold7d);
            if (!expired7dAccountIds.isEmpty()) {
                List<String> mongoIds = expired7dAccountIds.stream().map(UUID::toString).toList();
                userFeatureService.cleanUp7dFeatures(mongoIds);
                accountRepository.updateIs7dByAccountIds(expired7dAccountIds);
            }
        } catch (Exception e) {
            log.error("[Daily Reset Cron] Gặp lỗi nghiêm trọng khi dọn dẹp cho account: ", e);
        }
    }
}
