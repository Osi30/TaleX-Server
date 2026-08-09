package com.talex.server.schedulers;

import com.talex.server.services.creator.ICreatorSettlementService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class CreatorSettlementScheduler {

    private final ICreatorSettlementService creatorSettlementService;

    /**
     * Cron job chạy tự động vào 00:00:00 ngày 02 hàng tháng.
     * Expression: 0 0 0 2 * *
     */
    @Scheduled(cron = "0 0 0 2 * *")
    public void processMonthlyCreatorSettlementCron() {
        log.info("Starting scheduled monthly Creator Settlement calculation job (Day 2 of month)...");
        try {
            creatorSettlementService.processMonthlySettlement(false);
            log.info("Successfully completed monthly Creator Settlement calculation job.");
        } catch (Exception e) {
            log.error("Error executing monthly Creator Settlement calculation job: ", e);
        }
    }
}