package com.talex.server.services.trending.impls;

import com.talex.server.dtos.recommend.request.TrendingSampleConfigReq;
import com.talex.server.dtos.recommend.response.TrendingSampleConfigRes;
import com.talex.server.entities.config.TrendingSampleConfig;
import com.talex.server.enums.interaction.ImpressionStatus;
import com.talex.server.exceptions.details.ResourceNotFoundException;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.repositories.trending.TrendingSampleConfigRepository;
import com.talex.server.services.trending.TrendingSampleConfigService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.coyote.BadRequestException;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingSampleConfigServiceImpl implements TrendingSampleConfigService {

    private final TrendingSampleConfigRepository configRepository;
    private final SeriesRepository seriesRepository;

    private static final String CACHE_NAME = "trending_sample_config";
    private static final String CACHE_KEY = "'single_config'";

    /**
     * Lấy Cấu hình duy nhất. Lưu cache Redis 30 phút theo cấu hình mặc định của RedisCacheManager.
     */
    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CACHE_NAME, key = CACHE_KEY, cacheManager = "redisCacheManager")
    public TrendingSampleConfigRes getConfig() {
        TrendingSampleConfig config = configRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình TrendingSampleConfig chưa được khởi tạo trong hệ thống."));

        return TrendingSampleConfigRes.fromEntity(config);
    }

    /**
     * Khởi tạo cấu hình lần đầu. Kiểm tra nếu đã tồn tại row thì quăng lỗi.
     */
    @Override
    @Transactional
    public TrendingSampleConfigRes createConfig(TrendingSampleConfigReq req) throws BadRequestException {
        validateImpressionLimits(req.getMinImpression(), req.getMaxImpression());

        if (configRepository.count() > 0) {
            throw new BadRequestException("Cấu hình TrendingSampleConfig đã được khởi tạo trước đó. Vui lòng sử dụng tính năng Cập nhật.");
        }

        TrendingSampleConfig config = TrendingSampleConfig.builder()
                .minBatch(req.getMinBatch())
                .percentile(req.getPercentile())
                .minImpression(req.getMinImpression())
                .maxImpression(req.getMaxImpression())
                .confidenceScore(req.getConfidenceScore())
                .updatedAt(LocalDateTime.now())
                .build();

        TrendingSampleConfig saved = configRepository.save(config);
        return TrendingSampleConfigRes.fromEntity(saved);
    }

    /**
     * Cập nhật 4 trường chỉ định và tự động xỏa Redis Cache.
     */
    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = CACHE_KEY, cacheManager = "redisCacheManager")
    public TrendingSampleConfigRes updateConfig(TrendingSampleConfigReq req) throws BadRequestException {
        validateImpressionLimits(req.getMinImpression(), req.getMaxImpression());

        TrendingSampleConfig config = configRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Chưa có cấu hình nào trong hệ thống để cập nhật. Vui lòng tạo trước."));

        // Chỉ cập nhật 4 trường được phép
        config.setMinBatch(req.getMinBatch());
        config.setPercentile(req.getPercentile());
        config.setMinImpression(req.getMinImpression());
        config.setMaxImpression(req.getMaxImpression());
        config.setGravity(req.getGravity());
        config.setConfidenceScore(req.getConfidenceScore());
        config.setUpdatedAt(LocalDateTime.now());

        TrendingSampleConfig updated = configRepository.save(config);
        log.info("[TrendingConfig] Đã cập nhật cấu hình thành công & Xoá Redis Cache.");

        return TrendingSampleConfigRes.fromEntity(updated);
    }

    /// Được gọi ở hàm tăng số lượng ứng viên đã vượt qua vòng loại để tính toán lại bách phân vị khi đạt min batch
    @Override
    @Transactional
    @CacheEvict(value = "trending_sample_config", key = "'single_config'", cacheManager = "redisCacheManager")
    public void incrementBatchAndRecalculateThresholdIfNeeded(int completedCount) {
        TrendingSampleConfig config = configRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("TrendingSampleConfig chưa được khởi tạo."));

        config.setTotalBatch(config.getTotalBatch() + completedCount);
        int newCurrentBatch = config.getCurrentBatch() + completedCount;

        // Kiểm tra xem đã tích lũy đủ minBatch để tính lại Bách phân vị chưa
        if (newCurrentBatch >= config.getMinBatch()) {
            List<Double> historicalScores = seriesRepository.findTopLatestWilsonScoresSortedAsc(
                    List.of(ImpressionStatus.SUCCESS.name(), ImpressionStatus.FAILED.name()), config.getMinBatch()
            );

            double newThreshold = calculatePercentile(historicalScores, config.getPercentile());
            config.setThreshold(newThreshold);
            log.info("[TrendingConfig] Đã cập nhật Threshold mới = {} từ Bách phân vị P{}", newThreshold, config.getPercentile());

            // Reset batch hiện tại
            config.setCurrentBatch(0);
        } else {
            config.setCurrentBatch(newCurrentBatch);
        }

        config.setUpdatedAt(LocalDateTime.now());
        configRepository.save(config);
    }

    /**
     * Ép buộc tính toán lại Threshold ngay lập tức bởi Admin.
     * Reset currentBatch = 0, tính lại Threshold dựa trên toàn bộ lịch sử và cập nhật calculatedBatch.
     */
    @Override
    @Transactional
    @CacheEvict(value = CACHE_NAME, key = CACHE_KEY, cacheManager = "redisCacheManager")
    public TrendingSampleConfigRes forceRecalculateThreshold() {
        TrendingSampleConfig config = configRepository.findFirstBy()
                .orElseThrow(() -> new ResourceNotFoundException("Cấu hình TrendingSampleConfig chưa được khởi tạo."));

        // Lấy  lịch sử Wilson Scores của các Series đã hoàn thành Vòng 1 (SUCCESS & FAILED)
        List<Double> historicalScores = seriesRepository.findTopLatestWilsonScoresSortedAsc(
                List.of(ImpressionStatus.SUCCESS.name(), ImpressionStatus.FAILED.name()), config.getMinBatch()
        );

        // Tính toán Threshold mới theo Bách phân vị cấu hình
        double newThreshold = calculatePercentile(historicalScores, config.getPercentile());

        // Reset currentBatch về 0 và cập nhật calculatedBatch
        config.setThreshold(newThreshold);
        config.setCurrentBatch(0);
        config.setUpdatedAt(LocalDateTime.now());

        TrendingSampleConfig updated = configRepository.save(config);
        return TrendingSampleConfigRes.fromEntity(updated);
    }

    private void validateImpressionLimits(Long minImpression, Long maxImpression) throws BadRequestException {
        if (maxImpression < minImpression) {
            throw new BadRequestException("maxImpression phải lớn hơn hoặc bằng minImpression.");
        }
    }

    /**
     * Thuật toán tính Bách phân vị (Percentile) từ danh sách điểm đã sắp xếp
     */
    private double calculatePercentile(List<Double> sortedScores, double percentile) {
        if (sortedScores == null || sortedScores.isEmpty()) return 0.0;
        if (sortedScores.size() == 1) return sortedScores.getFirst();

        Collections.sort(sortedScores);
        double rank = (percentile / 100.0) * (sortedScores.size() - 1);
        int lowerIndex = (int) Math.floor(rank);
        int upperIndex = (int) Math.ceil(rank);

        if (lowerIndex == upperIndex) {
            return sortedScores.get(lowerIndex);
        }

        double weight = rank - lowerIndex;
        return sortedScores.get(lowerIndex) * (1 - weight) + sortedScores.get(upperIndex) * weight;
    }
}