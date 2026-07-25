package com.talex.server.services.trending;

public interface TrendingService {
    /**
     * Vòng 1: Đánh giá Wilson Score cho các Series đạt đủ Impression
     */
    void evaluateWilsonScoreBatch();

    /**
     * Cập nhật Ranking Score theo Hacker News Ranking công thức phân rã hàng giờ
     */
    void recalculateHackerNewsRankingScores();
}
