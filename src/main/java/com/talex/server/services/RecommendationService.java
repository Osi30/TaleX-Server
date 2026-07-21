package com.talex.server.services;

import com.talex.server.dtos.recommend.RankResultItem;

import java.util.List;

public interface RecommendationService {
    List<RankResultItem> getRecommendations(String accountId, List<String> seriesIds, String viewSessionId);

    List<String> getRecentWatchedSeries(String accountId);

    List<String> getSimilarSeriesIds(String seriesId);

    List<RankResultItem> rankSeries(String accountId, List<String> seriesIds);
}
