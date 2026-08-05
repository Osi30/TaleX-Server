package com.talex.server.schedulers;

import com.talex.server.services.recommend.SeriesPoolService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class SeriesChannelScheduler {
    private final SeriesPoolService poolService;

    /// Tiến trình lắp series pool
//    @Scheduled(cron = "0 0 * * * *")
    public void executeSeriesChannelPool() {
        try {
            poolService.rebuildAllGlobalPools();
        } catch (Exception e) {
            log.error("[SCHEDULED JOB] Lỗi khi kích hoạt tiến trình lắp lại pool series: ", e);
        }
    }
}
