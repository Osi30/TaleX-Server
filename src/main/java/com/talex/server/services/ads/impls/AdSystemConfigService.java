package com.talex.server.services.ads.impls;

import com.talex.server.dtos.requests.ads.InVideoConfigDto;
import com.talex.server.dtos.requests.ads.PopupConfigDto;
import com.talex.server.entities.ads.AdSystemConfig;
import com.talex.server.repositories.ads.AdSystemConfigRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdSystemConfigService {

    private static final Logger log = LoggerFactory.getLogger(AdSystemConfigService.class);

    private static final String POPUP_ROUTES_KEY = "POPUP_ALLOWED_ROUTES";
    private static final String POPUP_ROUTES_DEFAULT = "/,/series,/comics,/watch,/read,/intro,/missions,/profile,/bookmarks,/liked,/coin-history,/premium,/premium-history,/purchase-history,/subscriptions,/creator-channel,/public-channel,/recomment-demo";
    private static final String POPUP_DELAY_KEY = "POPUP_SHOW_DELAY_MS";
    private static final String POPUP_DELAY_DEFAULT = "3000";
    private static final String POPUP_COOLDOWN_KEY = "POPUP_COOLDOWN_MINUTES";
    private static final String POPUP_COOLDOWN_DEFAULT = "15";

    private static final String INVIDEO_SKIP_KEY = "INVIDEO_SKIP_AFTER_SEC";
    private static final String INVIDEO_SKIP_DEFAULT = "5";
    private static final String INVIDEO_COOLDOWN_KEY = "INVIDEO_COOLDOWN_SECONDS";
    private static final String INVIDEO_COOLDOWN_DEFAULT = "30";

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

    public InVideoConfigDto getInVideoConfig() {
        AdSystemConfig skipConfig = configRepository.findByConfigKey(INVIDEO_SKIP_KEY)
                .orElseGet(() -> seedConfig(INVIDEO_SKIP_KEY, INVIDEO_SKIP_DEFAULT, "Số giây bắt buộc xem trước khi hiện nút Skip của video"));
        AdSystemConfig cooldownConfig = configRepository.findByConfigKey(INVIDEO_COOLDOWN_KEY)
                .orElseGet(() -> seedConfig(INVIDEO_COOLDOWN_KEY, INVIDEO_COOLDOWN_DEFAULT, "Thời gian chống spam (giây) cho quảng cáo in-video"));

        return InVideoConfigDto.builder()
                .skipAfterSec(Integer.parseInt(skipConfig.getConfigValue()))
                .cooldownSeconds(Integer.parseInt(cooldownConfig.getConfigValue()))
                .build();
    }

    @Transactional
    public InVideoConfigDto updateInVideoConfig(InVideoConfigDto request) {
        if (request.getSkipAfterSec() == null || request.getSkipAfterSec() < 0) {
            throw new IllegalArgumentException("Skip after seconds must be >= 0");
        }
        if (request.getCooldownSeconds() == null || request.getCooldownSeconds() < 0) {
            throw new IllegalArgumentException("Cooldown seconds must be >= 0");
        }

        saveOrUpdateConfig(INVIDEO_SKIP_KEY, String.valueOf(request.getSkipAfterSec()), "Số giây bắt buộc xem trước khi hiện nút Skip của video");
        saveOrUpdateConfig(INVIDEO_COOLDOWN_KEY, String.valueOf(request.getCooldownSeconds()), "Thời gian chống spam (giây) cho quảng cáo in-video");

        return getInVideoConfig();
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

        String routesStr = String.join(",", request.getAllowedRoutes());
        saveOrUpdateConfig(POPUP_ROUTES_KEY, routesStr, "Danh sách routes được phép hiện Popup");
        saveOrUpdateConfig(POPUP_DELAY_KEY, String.valueOf(request.getShowDelayMs()), "Thời gian trễ (ms) trước khi hiện Popup");
        saveOrUpdateConfig(POPUP_COOLDOWN_KEY, String.valueOf(request.getCooldownMinutes()), "Thời gian cooldown (phút) sau khi đóng Popup");

        return getPopupConfig();
    }

    private void saveOrUpdateConfig(String key, String value, String desc) {
        AdSystemConfig config = configRepository.findByConfigKey(key)
                .orElse(AdSystemConfig.builder().configKey(key).build());
        config.setConfigValue(value);
        config.setDescription(desc);
        configRepository.save(config);
    }

    private AdSystemConfig seedConfig(String key, String value, String desc) {
        AdSystemConfig config = AdSystemConfig.builder()
                .configKey(key)
                .configValue(value)
                .description(desc)
                .build();
        return configRepository.save(config);
    }

    private AdSystemConfig seedDefaultPopupRoutes() {
        AdSystemConfig config = AdSystemConfig.builder()
                .configKey(POPUP_ROUTES_KEY)
                .configValue(POPUP_ROUTES_DEFAULT)
                .description("Danh sách routes được phép hiện Popup")
                .build();
        return configRepository.save(config);
    }

    private AdSystemConfig seedDefaultPopupDelay() {
        AdSystemConfig config = AdSystemConfig.builder()
                .configKey(POPUP_DELAY_KEY)
                .configValue(POPUP_DELAY_DEFAULT)
                .description("Thời gian trễ (ms) trước khi hiện Popup")
                .build();
        return configRepository.save(config);
    }

    private AdSystemConfig seedDefaultPopupCooldown() {
        AdSystemConfig config = AdSystemConfig.builder()
                .configKey(POPUP_COOLDOWN_KEY)
                .configValue(POPUP_COOLDOWN_DEFAULT)
                .description("Thời gian cooldown (phút) sau khi đóng Popup")
                .build();
        return configRepository.save(config);
    }
}
