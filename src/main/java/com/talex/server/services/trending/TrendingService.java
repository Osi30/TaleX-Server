package com.talex.server.services.trending;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.series.SeriesTrendingResponseDto;
import com.talex.server.enums.interaction.ImpressionStatus;

import java.util.List;

public interface TrendingService {
    /**
     * Vòng 1: Đánh giá Wilson Score cho các Series đạt đủ Impression
     */
    void evaluateWilsonScoreBatch();

    /**
     * Vòng 2: Cập nhật Ranking Score theo Hacker News Ranking công thức phân rã hàng giờ
     */
    void recalculateHackerNewsRankingScores();

    /**
     * Vòng 1: Lấy các ứng viên đang chờ thực hiện vòng 1
     */
    List<SeriesTrendingResponseDto> getCandidateNewReleasesSeriesIds(int page, int size);

    /**
     * Vòng 1: Lấy các ứng viên đã thực hiện vòng 1
     */
    BasePageResponse<SeriesTrendingResponseDto> getEvaluatedSeries(
            List<ImpressionStatus> statuses,
            int page,
            int size
    );

    /**
     * Vòng 1: Lấy các ứng viên đang thực hiện vòng 1
     */
    List<SeriesTrendingResponseDto> getNewReleasesPoolSeries();

    /**
     * Vòng 2: Lấy các ứng viên đang thực hiện vòng 2
     */
    List<SeriesTrendingResponseDto> getTrendingPoolSeries();
}
