package com.talex.server.services.ads.impls;

import com.talex.server.entities.ads.AdSystemConfig;
import com.talex.server.repositories.ads.AdSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.talex.server.dtos.requests.ads.PopupConfigDto;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AdSystemConfigService {

    private static final String POPUP_ROUTES_KEY = "POPUP_ALLOWED_ROUTES";
    private static final String POPUP_ROUTES_DEFAULT = "/,/series,/comics,/watch,/read,/intro,/missions,/profile,/bookmarks,/liked,/coin-history,/premium,/premium-history,/purchase-history,/subscriptions,/creator-channel,/public-channel,/recomment-demo";
    private static final String POPUP_DELAY_KEY = "POPUP_SHOW_DELAY_MS";
    private static final String POPUP_DELAY_DEFAULT = "3000";
    private static final String POPUP_COOLDOWN_KEY = "POPUP_COOLDOWN_MINUTES";
    private static final String POPUP_COOLDOWN_DEFAULT = "15";

    private final AdSystemConfigRepository configRepository;

    public PopupConfigDto getPopupConfig() {
        AdSystemConfig routesConfig = configRepository.findByConfigKey(POPUP_ROUTES_KEY)
                .orElseGet(this::seedDefaultPopupRoutes);
        
        AdSystemConfig delayConfig = configRepository.findByConfigKey(POPUP_DELAY_KEY)
                .orElseGet(this::seedDefaultPopupDelay);

        AdSystemConfig cooldownConfig = configRepository.findByConfigKey(POPUP_COOLDOWN_KEY)
                .orElseGet(this::seedDefaultPopupCooldown);

        List<String> routes = Arrays.asList(routesConfig.getConfigValue().split(","));
        Long delayMs = Long.parseLong(delayConfig.getConfigValue());
        Integer cooldown = Integer.parseInt(cooldownConfig.getConfigValue());

        return PopupConfigDto.builder()
                .allowedRoutes(routes)
                .showDelayMs(delayMs)
                .cooldownMinutes(cooldown)
                .build();
    }

    @Transactional
    public PopupConfigDto updatePopupConfig(PopupConfigDto request) {
        if (request.getAllowedRoutes() == null || request.getAllowedRoutes().isEmpty()) {
            throw new IllegalArgumentException("Routes list cannot be empty");
        }
        if (request.getShowDelayMs() == null || request.getShowDelayMs() < 0) {
            throw new IllegalArgumentException("Show delay ms must be >= 0");
        }
        if (request.getCooldownMinutes() == null || request.getCooldownMinutes() < 0) {
            throw new IllegalArgumentException("Cooldown minutes must be >= 0");
        }

        String routesValue = String.join(",", request.getAllowedRoutes().stream()
                .map(String::trim)
                .filter(r -> !r.isBlank())
                .toList());

        AdSystemConfig routesConfig = configRepository.findByConfigKey(POPUP_ROUTES_KEY)
                .orElseGet(() -> AdSystemConfig.builder()
                        .configKey(POPUP_ROUTES_KEY)
                        .description("Danh sách các route trên FE được phép hiển thị Popup Quảng Cáo")
                        .build());
        routesConfig.setConfigValue(routesValue);
        configRepository.save(routesConfig);

        AdSystemConfig delayConfig = configRepository.findByConfigKey(POPUP_DELAY_KEY)
                .orElseGet(() -> AdSystemConfig.builder()
                        .configKey(POPUP_DELAY_KEY)
                        .description("Thời gian chờ (ms) trước khi tự động hiện popup")
                        .build());
        delayConfig.setConfigValue(request.getShowDelayMs().toString());
        configRepository.save(delayConfig);

        AdSystemConfig cooldownConfig = configRepository.findByConfigKey(POPUP_COOLDOWN_KEY)
                .orElseGet(() -> AdSystemConfig.builder()
                        .configKey(POPUP_COOLDOWN_KEY)
                        .description("Thời gian chờ (phút) trước khi hiện lại popup sau khi user đã xem")
                        .build());
        cooldownConfig.setConfigValue(request.getCooldownMinutes().toString());
        configRepository.save(cooldownConfig);

        log.info("Admin updated POPUP config: routes={}, delay={}, cooldown={}", routesValue, request.getShowDelayMs(), request.getCooldownMinutes());

        return PopupConfigDto.builder()
                .allowedRoutes(Arrays.asList(routesValue.split(",")))
                .showDelayMs(request.getShowDelayMs())
                .cooldownMinutes(request.getCooldownMinutes())
                .build();
    }

    /**
     * Seed giá trị mặc định nếu chưa có trong DB.
     */
    private AdSystemConfig seedDefaultPopupRoutes() {
        AdSystemConfig defaultConfig = AdSystemConfig.builder()
                .configKey(POPUP_ROUTES_KEY)
                .configValue(POPUP_ROUTES_DEFAULT)
                .description("Danh sách các route trên FE được phép hiển thị Popup Quảng Cáo")
                .build();
        return configRepository.save(defaultConfig);
    }

    private AdSystemConfig seedDefaultPopupDelay() {
        AdSystemConfig defaultConfig = AdSystemConfig.builder()
                .configKey(POPUP_DELAY_KEY)
                .configValue(POPUP_DELAY_DEFAULT)
                .description("Thời gian chờ (ms) trước khi tự động hiện popup")
                .build();
        return configRepository.save(defaultConfig);
    }

    private AdSystemConfig seedDefaultPopupCooldown() {
        AdSystemConfig defaultConfig = AdSystemConfig.builder()
                .configKey(POPUP_COOLDOWN_KEY)
                .configValue(POPUP_COOLDOWN_DEFAULT)
                .description("Thời gian chờ (phút) trước khi hiện lại popup sau khi user đã xem")
                .build();
        return configRepository.save(defaultConfig);
    }
}
