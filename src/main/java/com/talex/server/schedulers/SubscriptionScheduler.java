package com.talex.server.schedulers;

import com.talex.server.records.WatchSessionResponseDto;
import com.talex.server.repositories.interaction.WatchSessionRepository;
import com.talex.server.services.subscription.SubscriptionStatService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SubscriptionScheduler {
    private final WatchSessionRepository watchSessionRepository;
    private final SubscriptionStatService subscriptionStatService;

    @Scheduled(cron = "0 0 0 * * *")
    public void processSubscriptionStats() {
        // Lấy danh sách watch session hợp lệ (watchDuration >= 5.0)
        List<WatchSessionResponseDto> validSessions = watchSessionRepository.findSessionsByMinWatchDuration(5.0);

        if (validSessions.isEmpty()) {
            log.info("No valid watch sessions found.");
            return;
        }

        int processedCount = 0;

        // Duyệt qua từng session và thực hiện upsert
        for (WatchSessionResponseDto session : validSessions) {
            if (session.accountId() == null) {
                continue;
            }

            try {
                subscriptionStatService.upsertSubscriptionStat(
                        session.accountId(),
                        session.creatorId(),
                        session.startTime()
                );
                processedCount++;
            } catch (Exception e) {
                log.error("Error processing subscription stat for watchSessionId: {}", session.watchSessionId(), e);
            }
        }

        log.info("Completed SubscriptionScheduler task. Processed {} valid sessions.", processedCount);
    }
}