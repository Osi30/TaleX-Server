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

    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicFeatureSync() {
        log.info("[SCHEDULED JOB] Kích hoạt tiến trình tự động đồng bộ Dynamic Features...");
        try {
            userFeatureService.syncUserDynamicFeatures();
            log.info("[SCHEDULED JOB] Hoàn thành luồng chạy.");
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi chạy ngầm: ", e);
        }
    }

    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicPreferencesSync() {
        log.info("[SCHEDULED JOB] Kích hoạt tự động đồng bộ Dynamic Preferences...");
        try {
            userFeatureService.syncUserDynamicPreferences();
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

        @Scheduled(cron = "0 0 0 * * ?")
    @Transactional
    public void processExpiredFeaturesCleanUp() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime threshold24h = now.minusHours(24);
        LocalDateTime threshold7d = now.minusDays(7);

        log.info("[CleanUp] Bắt đầu tiến trình dọn dẹp sliding window...");

        // --- XỬ LÝ 24 GIỜ ---
        List<UUID> expired24hAccountIds = accountRepository.findExpired24hAccountIds(threshold24h);
        if (!expired24hAccountIds.isEmpty()) {
            log.info("[24h] Tìm thấy {} tài khoản hết hạn.", expired24hAccountIds.size());

            // Convert List<UUID> sang List<String> phù hợp với _id của MongoDB Document
            List<String> mongoIds = expired24hAccountIds.stream().map(UUID::toString).toList();

            userFeatureService.cleanUp24hFeatures(mongoIds);
            accountRepository.updateIs24hByAccountIds(expired24hAccountIds);

            log.info("[24h] Đã reset Mongo và hạ cờ thành công.");
        }

        // --- XỬ LÝ 7 NGÀY ---
        List<UUID> expired7dAccountIds = accountRepository.findExpired7dAccountIds(threshold7d);
        if (!expired7dAccountIds.isEmpty()) {
            log.info("[7d] Tìm thấy {} tài khoản hết hạn.", expired7dAccountIds.size());

            List<String> mongoIds = expired7dAccountIds.stream().map(UUID::toString).toList();

            userFeatureService.cleanUp7dFeatures(mongoIds);
            accountRepository.updateIs7dByAccountIds(expired7dAccountIds);

            log.info("[7d] Đã reset Mongo và hạ cờ thành công.");
        }
    }
}
