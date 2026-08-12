package com.talex.server.dtos.recommend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class HomePoolsSeriesResponseDto {
    private List<SeriesCardResponseDto> promoted;
    private List<SeriesCardResponseDto> trending;
    private List<SeriesCardResponseDto> newReleases;
    private List<SeriesCardResponseDto> recentlyUpdated;
    private List<SeriesCardResponseDto> latestCommunityChoice;
    private List<SeriesCardResponseDto> communityChoice;
    private List<SeriesCardResponseDto> randomCategory;
    private List<SeriesCardResponseDto> accountSubscription;
}
