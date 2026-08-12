package com.talex.server.services.recommend;

import com.talex.server.dtos.recommend.request.HomeFeedRequestDto;
import com.talex.server.dtos.recommend.response.HomePoolsSeriesResponseDto;
import com.talex.server.dtos.recommend.response.PoolSeriesCardResponseDto;
import com.talex.server.dtos.recommend.response.RankResultItem;
import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;

import java.util.List;

public interface RecommendationService {
    HomePoolsSeriesResponseDto getHomeFeedSeries(String accountId, HomeFeedRequestDto request);

    List<SeriesCardResponseDto> getPersonalizedRecommendations(
            String accountId,
            String sessionId,
            String pageType, // "HOME" hoặc "DETAIL"
            int limit
    );

    List<PoolSeriesCardResponseDto> getLatestRecommendationPoolSeries(String accountId, String sessionId, String pageType);

    List<SeriesCardResponseDto> getAlreadyWatchedPoolSeries(String accountId);

    List<String> getRecentWatchedSeries(String accountId);

    List<String> getSimilarSeriesIds(String seriesId);

    List<RankResultItem> rankSeries(String accountId, List<String> seriesIds);
}
