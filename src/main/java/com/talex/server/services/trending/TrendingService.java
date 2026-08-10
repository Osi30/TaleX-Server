package com.talex.server.services.trending;

import com.talex.server.dtos.recommend.SeriesCardResponseDto;

import java.util.List;

public interface TrendingService {
    /**
     * Vòng 1: Đánh giá Wilson Score cho các Series đạt đủ Impression
     */
    void evaluateWilsonScoreBatch();

    /**
     * Vòng 1: Lấy các ứng viên đang chờ thực hiện vòng 1
     */
    List<SeriesCardResponseDto> getCandidateNewReleasesSeriesIds(int page, int size);

    /**
     * Cập nhật Ranking Score theo Hacker News Ranking công thức phân rã hàng giờ
     */
    void recalculateHackerNewsRankingScores();
}
