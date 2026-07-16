package com.talex.server.schedulers;

import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.mongo.ISeriesFeatureService;
import com.talex.server.services.mongo.IUserFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoFeatureSyncScheduler {
    private final IUserFeatureService syncService;
    private final ISeriesFeatureService seriesFeatureService;
    private final SeriesRepository seriesRepository;

    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicFeatureSync() {
        log.info("[SCHEDULED JOB] Kích hoạt tiến trình tự động đồng bộ Dynamic Features...");
        try {
            syncService.syncUserDynamicFeatures();
            log.info("[SCHEDULED JOB] Hoàn thành luồng chạy.");
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi chạy ngầm: ", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicPreferencesSync() {
        log.info("[SCHEDULED JOB] Kích hoạt tự động đồng bộ Dynamic Preferences...");
        try {
            syncService.syncUserDynamicPreferences();
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi luồng đồng bộ sở thích động: ", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void scheduleHourlySeriesStatsSync() {
        log.info("[Scheduler] Bắt đầu kích hoạt Cron Job đồng bộ định kỳ Series Features...");
        try {
            seriesFeatureService.syncAllSeriesFeatures();
            log.info("[Scheduler] Cron Job đồng bộ Series Features đã hoàn tất.");
        } catch (Exception e) {
            log.error("[Scheduler] Lỗi khi chạy Cron Job đồng bộ Series Features: ", e);
        }
    }

    @Scheduled(cron = "0 0 2 * * ?")
    public void runDailyInactiveSeriesReset() {
        log.info("[Daily Reset Cron] Bắt đầu quét các Series ngừng hoạt động...");
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold24h = now.minusHours(24);
        LocalDateTime threshold7d = now.minusDays(7);

        try {
            // 1. Xử lý reset stats 24h
            List<String> needs24hReset = seriesRepository.findSeriesIdsFor24hReset(threshold24h);
            if (!needs24hReset.isEmpty()) {
                log.info("[Daily Reset Cron] Tìm thấy {} series cần reset stats 24h.", needs24hReset.size());
                seriesFeatureService.resetInactiveSeriesStatsInMongo(needs24hReset, true, false);
                seriesRepository.markAs24hSynced(needs24hReset);
            }

            // 2. Xử lý reset stats 7d
            List<String> needs7dReset = seriesRepository.findSeriesIdsFor7dReset(threshold7d);
            if (!needs7dReset.isEmpty()) {
                log.info("[Daily Reset Cron] Tìm thấy {} series cần reset stats 7d.", needs7dReset.size());
                seriesFeatureService.resetInactiveSeriesStatsInMongo(needs7dReset, false, true);
                seriesRepository.markAs7dSynced(needs7dReset);
            }

            log.info("[Daily Reset Cron] Hoàn tất tiến trình quét dọn stats inactive thành công.");
        } catch (Exception e) {
            log.error("[Daily Reset Cron] Gặp lỗi nghiêm trọng khi reset stats inactive: ", e);
        }
    }
}
