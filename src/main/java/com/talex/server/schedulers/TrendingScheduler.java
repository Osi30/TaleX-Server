package com.talex.server.schedulers;

import com.talex.server.services.trending.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class TrendingScheduler {
    private final TrendingService trendingService;

    /**
     * Cron Job 1: Kiểm tra và đánh giá Wilson Score Vòng 1 định kỳ mỗi 1 tiếng.
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleWilsonScoreEvaluation() {
        log.info("[CronJob] Start process Wilson Score evaluation...");
        try {
            trendingService.evaluateWilsonScoreBatch();
        } catch (Exception e) {
            log.error("[CronJob] Error evaluating Wilson Score: ", e);
        }
    }

    /**
     * Cron Job 2: Cập nhật lại Hacker News Ranking Score định kỳ mỗi 1 tiếng (vào đầu mỗi giờ).
     */
    @Scheduled(cron = "0 0 * * * *")
    public void scheduleHackerNewsRankingScoreUpdate() {
        log.info("[CronJob] Start process Hacker News Ranking Score update...");
        try {
            trendingService.recalculateHackerNewsRankingScores();
        } catch (Exception e) {
            log.error("[CronJob] Error updating Ranking Score: ", e);
        }
    }
}
