package com.talex.server.schedulers;

import com.talex.server.services.ads.AdCampaignService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdCampaignScheduler {

    private final AdCampaignService adCampaignService;

    // Run every 5 minutes
    @Scheduled(fixedDelay = 300_000)
    public void processAdCampaignLifecycle() {
        log.info("Starting Ad Campaign lifecycle processing...");
        try {
            adCampaignService.processCampaignLifecycle();
            log.info("Finished Ad Campaign lifecycle processing.");
        } catch (Exception e) {
            log.error("Error during Ad Campaign lifecycle processing: ", e);
        }
    }
}
