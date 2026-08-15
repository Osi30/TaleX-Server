package com.talex.server.configs;

import com.talex.server.entities.ads.AdSlot;
import com.talex.server.enums.ads.AdSlotType;
import com.talex.server.repositories.ads.AdSlotRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AdSlotSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AdSlotSeeder.class);
    private final AdSlotRepository slotRepository;

    @Override
    public void run(String... args) throws Exception {
        // Automatically delete HOME_BANNER if exists
        slotRepository.findByCodeName("HOME_BANNER").ifPresent(slot -> {
            log.info("Removing unused HOME_BANNER slot from database...");
            slotRepository.delete(slot);
        });

        // Seed POPUP_OVERLAY if missing
        if (slotRepository.findByCodeName("POPUP_OVERLAY").isEmpty()) {
            AdSlot popupOverlay = AdSlot.builder()
                    .codeName("POPUP_OVERLAY")
                    .displayName("Popup Overlay")
                    .type(AdSlotType.POPUP)
                    .price(50000L)
                    .totalViewOfPrice(1000L)
                    .isActive(true)
                    .isServingEnabled(true)
                    .build();
            slotRepository.save(popupOverlay);
            log.info("POPUP_OVERLAY slot seeded.");
        }

        // Seed IN_VIDEO if missing
        if (slotRepository.findByCodeName("IN_VIDEO").isEmpty()) {
            AdSlot inVideo = AdSlot.builder()
                    .codeName("IN_VIDEO")
                    .displayName("Quảng cáo trong Video")
                    .type(AdSlotType.VIDEO)
                    .price(200000L)
                    .totalViewOfPrice(1000L)
                    .isActive(true)
                    .isServingEnabled(true)
                    .build();
            slotRepository.save(inVideo);
            log.info("IN_VIDEO slot seeded.");
        }
    }
}
