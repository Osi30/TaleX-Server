package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.recommend.response.SeriesChannelConfigRes;
import com.talex.server.services.recommend.SeriesChannelConfigService;
import com.talex.server.services.recommend.SeriesChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesPoolServiceImpl Tests")
class SeriesPoolServiceImplTest {

    @Mock
    private SeriesChannelService seriesChannelService;

    @Mock
    private SeriesChannelConfigService configService;

    @InjectMocks
    private SeriesPoolServiceImpl seriesPoolService;

    private SeriesChannelConfigRes sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = SeriesChannelConfigRes.builder()
                .promotedPoolNumber(5)
                .newReleasedPoolNumber(10)
                .trendingPoolNumber(10)
                .latestCommunityChoicePoolNumber(10)
                .communityChoicePoolNumber(10)
                .recentlyUpdatedPoolNumber(10)
                .randomCategoryPoolNumber(15)
                .numberPerCategory(3)
                .build();
    }

    @Test
    @DisplayName("rebuildAllGlobalPools - Success execution flow")
    void rebuildAllGlobalPools_Success() {
        when(configService.getConfig()).thenReturn(sampleConfig);
        when(seriesChannelService.refreshPromotedPool(5)).thenReturn(List.of("series-1", "series-2"));
        when(seriesChannelService.refreshNewReleasesPool(anyList(), eq(10))).thenReturn(List.of("series-3"));
        when(seriesChannelService.refreshTrendingPool(anyList(), eq(10))).thenReturn(List.of("series-4"));
        when(seriesChannelService.refreshLatestCommunityChoicePool(anyList(), eq(10))).thenReturn(List.of("series-5"));
        when(seriesChannelService.refreshCommunityChoicePool(anyList(), eq(10))).thenReturn(List.of("series-6"));
        when(seriesChannelService.refreshRecentlyUpdatedPool(anyList(), eq(10))).thenReturn(List.of("series-7"));
        when(seriesChannelService.refreshRandomCategoryPool(anyList(), eq(3), eq(15))).thenReturn(List.of("series-8"));

        seriesPoolService.rebuildAllGlobalPools();

        verify(configService, times(1)).getConfig();
        verify(seriesChannelService, times(1)).refreshPromotedPool(5);
        verify(seriesChannelService, times(1)).refreshNewReleasesPool(anyList(), eq(10));
        verify(seriesChannelService, times(1)).refreshTrendingPool(anyList(), eq(10));
        verify(seriesChannelService, times(1)).refreshLatestCommunityChoicePool(anyList(), eq(10));
        verify(seriesChannelService, times(1)).refreshCommunityChoicePool(anyList(), eq(10));
        verify(seriesChannelService, times(1)).refreshRecentlyUpdatedPool(anyList(), eq(10));
        verify(seriesChannelService, times(1)).refreshRandomCategoryPool(anyList(), eq(3), eq(15));

        verify(seriesChannelService, times(1)).updateGlobalIds(argThat(set ->
                set.containsAll(Set.of("series-1", "series-2", "series-3", "series-4", "series-5", "series-6", "series-7", "series-8"))
        ));
    }

    @Test
    @DisplayName("rebuildAllGlobalPools - Exception handling flow")
    void rebuildAllGlobalPools_ExceptionHandled() {
        when(configService.getConfig()).thenThrow(new RuntimeException("Database error"));

        // Should not throw exception
        seriesPoolService.rebuildAllGlobalPools();

        verify(configService, times(1)).getConfig();
        verify(seriesChannelService, never()).refreshPromotedPool(anyInt());
        verify(seriesChannelService, never()).updateGlobalIds(any());
    }
}
