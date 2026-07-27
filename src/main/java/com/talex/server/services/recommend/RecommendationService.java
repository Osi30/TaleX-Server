package com.talex.server.services.recommend;

import com.talex.server.dtos.recommend.HomePoolsSeriesResponseDto;
import com.talex.server.dtos.recommend.RankResultItem;
import com.talex.server.dtos.recommend.SeriesCardResponseDto;

import java.util.List;

public interface RecommendationService {
    HomePoolsSeriesResponseDto getHomeFeedSeries(String accountId, int limitPerPool);

    List<SeriesCardResponseDto> getPersonalizedRecommendations(
            String accountId,
            String sessionId,
            String pageType, // "HOME" hoặc "DETAIL"
            int limit
    );

    List<RankResultItem> getRecommendations(String accountId, List<String> seriesIds, String viewSessionId);

    List<String> getRecentWatchedSeries(String accountId);

    List<String> getSimilarSeriesIds(String seriesId);

    List<RankResultItem> rankSeries(String accountId, List<String> seriesIds);
}
