package com.talex.server.schedulers;

import com.talex.server.services.mongo.IUserFeatureService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class MongoFeatureSyncScheduler {
    private final IUserFeatureService syncService;

//    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicFeatureSync() {
        log.info("[SCHEDULED JOB] Kích hoạt tiến trình tự động đồng bộ Dynamic Features...");
        try {
            syncService.syncUserDynamicFeatures();
            log.info("[SCHEDULED JOB] Hoàn thành luồng chạy.");
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi chạy ngầm: ", e);
        }
    }

    //    @Scheduled(cron = "0 0 * * * *")
    public void executeDynamicPreferencesSync() {
        log.info("[SCHEDULED JOB] Kích hoạt tự động đồng bộ Dynamic Preferences...");
        try {
            syncService.syncUserDynamicPreferences();
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi luồng đồng bộ sở thích động: ", e);
        }
    }
}
