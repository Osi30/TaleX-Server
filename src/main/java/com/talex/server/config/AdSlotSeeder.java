package com.talex.server.config;

import com.talex.server.entities.ads.AdSlot;
import com.talex.server.enums.ads.AdSlotType;
import com.talex.server.repositories.ads.AdSlotRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdSlotSeeder implements CommandLineRunner {

    private final AdSlotRepository slotRepository;

    @Override
    public void run(String... args) throws Exception {
        if (slotRepository.count() < 3) {
            log.info("Seeding Ad Slots for TikTok Ads Manager style...");
            slotRepository.deleteAll(); // Wipe old testing slots

            AdSlot homeBanner = AdSlot.builder()
                    .codeName("HOME_BANNER")
                    .displayName("Banner Trang Chủ")
                    .type(AdSlotType.BANNER)
                    .price(100000L)
                    .totalViewOfPrice(1000L)
                    .isActive(true)
                    .build();

            AdSlot thumbnail = AdSlot.builder()
                    .codeName("THUMBNAIL")
                    .displayName("Thumbnail Banner")
                    .type(AdSlotType.BANNER)
                    .price(50000L)
                    .totalViewOfPrice(1000L)
                    .isActive(true)
                    .build();

            AdSlot inVideo = AdSlot.builder()
                    .codeName("IN_VIDEO")
                    .displayName("Quảng cáo trong Video")
                    .type(AdSlotType.VIDEO)
                    .price(200000L)
                    .totalViewOfPrice(1000L)
                    .isActive(true)
                    .build();

            slotRepository.save(homeBanner);
            slotRepository.save(thumbnail);
            slotRepository.save(inVideo);
            log.info("Ad Slots seeded successfully.");
        }
    }
}
