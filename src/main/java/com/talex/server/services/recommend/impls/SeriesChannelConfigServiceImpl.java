package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.recommend.request.SeriesChannelConfigReq;
import com.talex.server.dtos.recommend.response.SeriesChannelConfigRes;
import com.talex.server.entities.config.SeriesChannelConfig;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.trending.SeriesChannelConfigRepository;
import com.talex.server.services.recommend.SeriesChannelConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
@Slf4j
public class SeriesChannelConfigServiceImpl implements SeriesChannelConfigService {

    private final SeriesChannelConfigRepository configRepository;

    private static final String CACHE_NAME = "series_channel_config";
    private static final String CACHE_KEY = "'single_config'";

    /**
     * Lấy Cấu hình duy nhất. Lưu cache Redis theo cấu hình mặc định của RedisCacheManager.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = CACHE_KEY, cacheManager = "redisCacheManager")
    public SeriesChannelConfigRes getConfig() {
        SeriesChannelConfig config = configRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình SeriesChannelConfig chưa được khởi tạo trong hệ thống."));

        return SeriesChannelConfigRes.fromEntity(config);
    }

    /**
     * Khởi tạo cấu hình lần đầu. Kiểm tra nếu đã tồn tại row thì quăng lỗi.
     */
    @Override
    @Transactional
    public SeriesChannelConfigRes createConfig(SeriesChannelConfigReq req) throws BadRequestException {
        if (configRepository.count() > 0) {
            throw new BadRequestException("Cấu hình SeriesChannelConfig đã được khởi tạo trước đó. Vui lòng sử dụng tính năng Cập nhật.");
        }

        SeriesChannelConfig config = SeriesChannelConfig.builder()
                .trendingPoolNumber(req.getTrendingPoolNumber())
                .promotedPoolNumber(req.getPromotedPoolNumber())
                .newReleasedPoolNumber(req.getNewReleasedPoolNumber())
                .latestCommunityChoicePoolNumber(req.getLatestCommunityChoicePoolNumber())
                .communityChoicePoolNumber(req.getCommunityChoicePoolNumber())
                .recentlyUpdatedPoolNumber(req.getRecentlyUpdatedPoolNumber())
                .randomCategoryPoolNumber(req.getRandomCategoryPoolNumber())
                .subscribedPoolNumber(req.getSubscribedPoolNumber())
                .numberPerCategory(req.getNumberPerCategory())
                .updatedAt(LocalDateTime.now())
                .build();

        SeriesChannelConfig saved = configRepository.save(config);
        return SeriesChannelConfigRes.fromEntity(saved);
    }

    /**
     * Cập nhật các trường cấu hình và tự động xỏa Redis Cache.
     */
    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = CACHE_KEY, cacheManager = "redisCacheManager")
    public SeriesChannelConfigRes updateConfig(SeriesChannelConfigReq req) throws BadRequestException {
        SeriesChannelConfig config = configRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cấu hình SeriesChannelConfig trong hệ thống để cập nhật. Vui lòng tạo trước."));

        config.setTrendingPoolNumber(req.getTrendingPoolNumber());
        config.setPromotedPoolNumber(req.getPromotedPoolNumber());
        config.setNewReleasedPoolNumber(req.getNewReleasedPoolNumber());
        config.setLatestCommunityChoicePoolNumber(req.getLatestCommunityChoicePoolNumber());
        config.setCommunityChoicePoolNumber(req.getCommunityChoicePoolNumber());
        config.setRecentlyUpdatedPoolNumber(req.getRecentlyUpdatedPoolNumber());
        config.setRandomCategoryPoolNumber(req.getRandomCategoryPoolNumber());
        config.setSubscribedPoolNumber(req.getSubscribedPoolNumber());
        config.setNumberPerCategory(req.getNumberPerCategory());
        config.setUpdatedAt(LocalDateTime.now());

        SeriesChannelConfig updated = configRepository.save(config);
        log.info("[SeriesChannelConfig] Đã cập nhật cấu hình thành công & Xóa Redis Cache.");

        return SeriesChannelConfigRes.fromEntity(updated);
    }
}