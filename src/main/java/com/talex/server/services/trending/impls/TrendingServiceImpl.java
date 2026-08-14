package com.talex.server.services.trending.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.recommend.response.TrendingSampleConfigRes;
import com.talex.server.dtos.responses.series.SeriesTrendingResponseDto;
import com.talex.server.entities.analytic.TrendingAnalyticData;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.interaction.ImpressionStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.mappers.series.SeriesMapper;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.recommend.SeriesChannelService;
import com.talex.server.services.trending.TrendingSampleConfigService;
import com.talex.server.services.trending.TrendingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class TrendingServiceImpl implements TrendingService {
    private final SeriesRepository seriesRepository;
    private final SeriesMapper seriesMapper;
    private final TrendingSampleConfigService configService;
    private final SeriesChannelService seriesChannelService;

    @Override
    @Transactional
    public void evaluateWilsonScoreBatch() {
        TrendingSampleConfigRes config = configService.getConfig();

        // 1. Lấy danh sách Series đang ON_GOING và có totalImpression >= minImpression
        List<Series> candidates = seriesRepository
                .findCandidateWilsonSeries(
                        config.getMinImpression(),
                        SeriesStatus.PUBLISHED,
                        ImpressionStatus.ON_GOING
                );
        if (candidates.isEmpty()) {
            return;
        }

        List<Series> updatedSeriesList = new ArrayList<>();
        int evaluatedCount = 0;

        for (Series series : candidates) {
            TrendingAnalyticData analytic = series.getTrendingAnalyticData();
            long totalImpression = analytic.getTotalImpression();
            long engageClick = analytic.getEngageClick();

            // Tính p_hat (sampleRatio) = engageClick / totalImpression
            double sampleRatio = totalImpression > 0 ? (double) engageClick / totalImpression : 0.0;
            analytic.setSampleRatio(sampleRatio);

            // Tính Wilson Score
            double wilsonScore = calculateWilsonScore(engageClick, totalImpression);
            analytic.setWilsonScore(wilsonScore);

            boolean isEvaluated = false;

            // Đánh giá trạng thái Vòng 1
            if (wilsonScore >= config.getThreshold()) {
                analytic.setImpressionStatus(ImpressionStatus.SUCCESS);
                analytic.setWilsonUpdatedAt(LocalDateTime.now());
                isEvaluated = true;

                // Tính ngay Hacker News Ranking Score cho Series SUCCESS
                double initialRankingScore = calculateHackerNewsRankingScore(
                        series.getRatingCount(),
                        analytic.getInteractionClick(),
                        analytic.getEngageClick(),
                        series.getReleasedUpdateTime(),
                        config.getGravity()
                );
                analytic.setRankingScore(initialRankingScore);

            } else if (totalImpression >= config.getMaxImpression()) {
                // Nếu vượt maxImpression mà điểm vẫn < threshold -> FAILED
                analytic.setImpressionStatus(ImpressionStatus.FAILED);
                analytic.setWilsonUpdatedAt(LocalDateTime.now());
                isEvaluated = true;
            }

            if (isEvaluated) {
                evaluatedCount++;
                updatedSeriesList.add(series);
            }
        }

        // 2. Lưu các Series đã cập nhật
        if (!updatedSeriesList.isEmpty()) {
            seriesRepository.saveAll(updatedSeriesList);
            log.info("[WilsonEvaluation] Đã đánh giá xong {} series trong Vòng 1.", evaluatedCount);

            // 3. Cập nhật currentBatch, totalBatch và tính lại threshold nếu đạt minBatch
            configService.incrementBatchAndRecalculateThresholdIfNeeded(evaluatedCount);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesTrendingResponseDto> getCandidateNewReleasesSeriesIds(int page, int size) {
        // 1. Lấy Max Impression từ TrendingSampleConfigService
        TrendingSampleConfigRes config = configService.getConfig();
        Long maxImpression = config.getMaxImpression();

        // 2. Lấy toàn bộ phần tử trong Redis pool:new_releases để làm Blacklist
        List<String> blacklist = seriesChannelService.getNewReleasesPoolElements();
        boolean isBlacklistEmpty = blacklist.isEmpty();

        // 3. Phân trang
        Pageable pageable = PageRequest.of(page, size);

        // 4. Gọi Repository lấy danh sách Candidate Series IDs
        return seriesRepository.findNewSeriesWaitedForDistribution(
                SeriesStatus.PUBLISHED,
                maxImpression,
                blacklist,
                isBlacklistEmpty,
                ImpressionStatus.ON_GOING,
                pageable
        ).stream().map(s -> toTrendingDto(s, config.getGravity())).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<SeriesTrendingResponseDto> getNewReleasesPoolSeries() {
        // Lấy danh sách Series IDs đang có trong Redis pool:new_releases
        List<String> seriesIds = seriesChannelService.getNewReleasesPoolElements();
        if (seriesIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Series> series = seriesRepository.findAllBySeriesIdIn(seriesIds);
        TrendingSampleConfigRes config = configService.getConfig();

        return series.stream()
                .map(s -> toTrendingDto(s, config.getGravity()))
                .toList();
    }

    @Override
    public List<SeriesTrendingResponseDto> getTrendingPoolSeries() {
        // Lấy danh sách Series IDs đang có trong Redis pool:new_releases
        List<String> seriesIds = seriesChannelService.getTrendingPoolElements();
        if (seriesIds.isEmpty()) {
            return Collections.emptyList();
        }

        List<Series> series = seriesRepository.findAllBySeriesIdIn(seriesIds);
        TrendingSampleConfigRes config = configService.getConfig();

        return series.stream()
                .map(s -> toTrendingDto(s, config.getGravity()))
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BasePageResponse<SeriesTrendingResponseDto> getEvaluatedSeries(
            List<ImpressionStatus> statuses,
            int page,
            int size
    ) {
        // Mặc định nếu người dùng không truyền danh sách status thì lấy cả SUCCESS và FAILED
        if (statuses == null || statuses.isEmpty()) {
            statuses = List.of(ImpressionStatus.SUCCESS, ImpressionStatus.FAILED);
        }

        Pageable pageable = PageRequest.of(page, size);
        TrendingSampleConfigRes config = configService.getConfig();

        Page<Series> seriesPage = seriesRepository.findEvaluatedWilsonSeries(
                SeriesStatus.PUBLISHED,
                statuses,
                pageable
        );

        List<SeriesTrendingResponseDto> content = seriesPage.getContent().stream()
                .map(s -> toTrendingDto(s, config.getGravity()))
                .toList();

        return BasePageResponse.<SeriesTrendingResponseDto>builder()
                .content(content)
                .pageNumber(seriesPage.getNumber())
                .pageSize(seriesPage.getSize())
                .totalElements(seriesPage.getTotalElements())
                .totalPages(seriesPage.getTotalPages())
                .build();
    }

    @Override
    @Transactional
    public void recalculateHackerNewsRankingScores() {
        TrendingSampleConfigRes config = configService.getConfig();
        List<Series> successSeriesList = seriesRepository.findSuccessTrendingSeries(SeriesStatus.PUBLISHED);

        if (successSeriesList.isEmpty()) {
            return;
        }

        for (Series series : successSeriesList) {
            TrendingAnalyticData analytic = series.getTrendingAnalyticData();

            double newRankingScore = calculateHackerNewsRankingScore(
                    series.getRatingCount(),
                    analytic.getInteractionClick(),
                    analytic.getEngageClick(),
                    series.getReleasedUpdateTime(),
                    config.getGravity()
            );

            analytic.setRankingScore(newRankingScore);
        }

        seriesRepository.saveAll(successSeriesList);
        log.info("[RankingScore] Đã cập nhật Ranking Score cho {} series SUCCESS thành công.", successSeriesList.size());
    }

    private SeriesTrendingResponseDto toTrendingDto(Series series, Double gravity) {
        if (series == null) {
            return null;
        }

        TrendingAnalyticData trendingAnalytic = series.getTrendingAnalyticData();
        long engageClick = trendingAnalytic != null ? trendingAnalytic.getEngageClick() : 0L;
        long totalImpression = trendingAnalytic != null ? trendingAnalytic.getTotalImpression() : 0L;
        long interactionClick = trendingAnalytic != null ? trendingAnalytic.getInteractionClick() : 0L;
        double sampleRatio = totalImpression > 0 ? (double) engageClick / totalImpression : 0.0;

        // Tính điểm Realtime khi Admin gọi API xem danh sách
        double realtimeWilsonScore = calculateWilsonScore(engageClick, totalImpression);
        double realtimeUpperWilsonScore = calculateUpperWilsonScore(engageClick, totalImpression);
        double realtimeRankingScore = calculateHackerNewsRankingScore(
                series.getRatingCount(),
                interactionClick,
                engageClick,
                series.getReleasedUpdateTime(),
                gravity
        );

        SeriesTrendingResponseDto dto = seriesMapper.toTrendingDto(series);
        dto.getTrendingAnalyticData().setSampleRatio(sampleRatio);
        dto.setWilsonScore(realtimeWilsonScore);
        dto.setUpperWilsonScore(realtimeUpperWilsonScore);
        dto.setRankingScore(realtimeRankingScore);
        return dto;
    }

    /**
     * Công thức khoảng tin cậy Wilson (Wilson Score Lower Bound) - Độ tin cậy 95% (z = 1.96)
     */
    private double calculateWilsonScore(long engageClick, long totalImpression) {
        if (totalImpression <= 0) return 0.0;

        double p = (double) engageClick / totalImpression;
        double n = totalImpression;
        double z = 1.96; // 95% confidence level

        double z2 = z * z;
        double denominator = 1.0 + z2 / n;
        double p1 = p + z2 / (2.0 * n);
        double p2 = z * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n);

        double score = (p1 - p2) / denominator;
        return Math.max(0.0, score);
    }

    /**
     * Công thức khoảng tin cậy Wilson (Wilson Score Upper Bound) - Cận trên (z = 1.96)
     */
    private double calculateUpperWilsonScore(long engageClick, long totalImpression) {
        if (totalImpression <= 0) return 0.0;

        double p = (double) engageClick / totalImpression;
        double n = totalImpression;
        double z = 1.96; // 95% confidence level

        double z2 = z * z;
        double denominator = 1.0 + z2 / n;
        double p1 = p + z2 / (2.0 * n);
        double p2 = z * Math.sqrt((p * (1.0 - p) + z2 / (4.0 * n)) / n);

        double score = (p1 + p2) / denominator;
        return Math.min(1.0, score);
    }

    /**
     * Công thức Hacker News Ranking Score: Score = (P) / (T + 2)^G
     * P = ratingCount + interactionClick + engageClick
     * T = Số giờ kể từ thời điểm releasedUpdateTime
     * G = Hệ số phân rã (gravity = 1.8)
     */
    private double calculateHackerNewsRankingScore(
            Long ratingCount,
            Long interactionClick,
            Long engageClick,
            LocalDateTime releasedUpdateTime,
            Double gravity) {

        long p = (ratingCount != null ? ratingCount : 0L)
                + (interactionClick != null ? interactionClick : 0L)
                + (engageClick != null ? engageClick : 0L);

        LocalDateTime baseTime = releasedUpdateTime != null ? releasedUpdateTime : LocalDateTime.now();
        long hours = ChronoUnit.HOURS.between(baseTime, LocalDateTime.now());
        if (hours < 0) hours = 0;

        double g = (gravity != null && gravity > 0) ? gravity : 1.8;
        double denominator = Math.pow(hours + 2.0, g);

        return (double) p / denominator;
    }
}
