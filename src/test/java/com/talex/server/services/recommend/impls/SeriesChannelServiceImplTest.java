package com.talex.server.services.recommend.impls;

import com.talex.server.dtos.mongo.UserDynamicFeature;
import com.talex.server.dtos.mongo.UserStaticFeature;
import com.talex.server.dtos.recommend.response.TrendingSampleConfigRes;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.interaction.ImpressionStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.repositories.series.SeriesLogRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.mongo.UserFeatureService;
import com.talex.server.services.trending.TrendingSampleConfigService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeriesChannelServiceImpl Tests")
class SeriesChannelServiceImplTest {

    @Mock
    private UserFeatureService userFeatureService;
    @Mock
    private TrendingSampleConfigService trendingSampleConfigService;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private SeriesLogRepository seriesLogRepository;
    @Mock
    private CampaignSeriesRepository campaignSeriesRepository;
    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ListOperations<String, String> listOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    @InjectMocks
    private SeriesChannelServiceImpl channelService;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);
    }

    // =========================================================================
    // 1. PROMOTED CHANNEL
    // =========================================================================

    @Test
    @DisplayName("getPromotedPoolElements - returns elements when present or empty when null")
    void getPromotedPoolElements() {
        when(listOperations.range("pool:promoted", 0, -1)).thenReturn(List.of("p1", "p2")).thenReturn(null);

        List<String> res1 = channelService.getPromotedPoolElements();
        assertThat(res1).containsExactly("p1", "p2");

        List<String> res2 = channelService.getPromotedPoolElements();
        assertThat(res2).isEmpty();
    }

    @Test
    @DisplayName("getPromotedSeriesIds - limit <= 0 or empty pool")
    void getPromotedSeriesIds_InvalidInputs() {
        assertThat(channelService.getPromotedSeriesIds("user1", 0)).isEmpty();

        when(listOperations.size("pool:promoted")).thenReturn(null);
        assertThat(channelService.getPromotedSeriesIds("user1", 5)).isEmpty();

        when(listOperations.size("pool:promoted")).thenReturn(0L);
        assertThat(channelService.getPromotedSeriesIds("user1", 5)).isEmpty();
    }

    @Test
    @DisplayName("getPromotedSeriesIds - Normal and Circular Offset calculation")
    void getPromotedSeriesIds_WithOffsetAndWrapAround() {
        when(listOperations.size("pool:promoted")).thenReturn(5L);
        // Case 1: offset is 0, fetch 3 elements -> range(0, 2)
        when(valueOperations.get("offset:promoted:user1")).thenReturn("0");
        when(listOperations.range("pool:promoted", 0L, 2L)).thenReturn(List.of("p1", "p2", "p3"));

        List<String> res1 = channelService.getPromotedSeriesIds("user1", 3);
        assertThat(res1).containsExactly("p1", "p2", "p3");
        verify(valueOperations).set("offset:promoted:user1", "3");

        // Case 2: offset is 3, fetch 3 elements (crosses poolSize 5) -> range(3, 4) + range(0, 0)
        when(valueOperations.get("offset:promoted:user1")).thenReturn("3");
        when(listOperations.range("pool:promoted", 3L, 4L)).thenReturn(List.of("p4", "p5"));
        when(listOperations.range("pool:promoted", 0L, 0L)).thenReturn(List.of("p1"));

        List<String> res2 = channelService.getPromotedSeriesIds("user1", 3);
        assertThat(res2).containsExactly("p4", "p5", "p1");
        verify(valueOperations).set("offset:promoted:user1", "1");

        // Case 3: Invalid number format offset or offset >= poolSize
        when(valueOperations.get("offset:promoted:user1")).thenReturn("invalid");
        when(listOperations.range("pool:promoted", 0L, 1L)).thenReturn(List.of("p1", "p2"));

        List<String> res3 = channelService.getPromotedSeriesIds("user1", 2);
        assertThat(res3).containsExactly("p1", "p2");
    }

    @Test
    @DisplayName("refreshPromotedPool - merges old and new IDs correctly")
    void refreshPromotedPool() {
        // Old pool has ["old1", "old2"]
        when(listOperations.range("pool:promoted", 0, -1)).thenReturn(List.of("old1", "old2"));
        // DB returns new fetched ["new1"]
        when(campaignSeriesRepository.findActivePromotedSeriesIds(eq(CampaignStatus.RUNNING), eq(List.of("old1", "old2")), eq(false), any(Pageable.class)))
                .thenReturn(List.of("new1"));

        List<String> merged = channelService.refreshPromotedPool(3);

        // newFetched.size() (1) < limit (3) -> remainder 2 -> retain 2 from old ["old1", "old2"] + ["new1"] = ["old1", "old2", "new1"]
        assertThat(merged).containsExactly("old1", "old2", "new1");
        verify(redisTemplate).delete("pool:promoted");
        verify(listOperations).rightPushAll("pool:promoted", merged);
        verify(redisTemplate).expire(eq("pool:promoted"), any());
    }

    @Test
    @DisplayName("refreshPromotedPool - when new fetched exceeds or equals limit")
    void refreshPromotedPool_NewFetchedExceedsLimit() {
        when(listOperations.range("pool:promoted", 0, -1)).thenReturn(null);
        when(campaignSeriesRepository.findActivePromotedSeriesIds(eq(CampaignStatus.RUNNING), eq(Collections.emptyList()), eq(true), any(Pageable.class)))
                .thenReturn(List.of("n1", "n2", "n3", "n4"));

        List<String> merged = channelService.refreshPromotedPool(2);

        assertThat(merged).containsExactly("n1", "n2");
    }

    // =========================================================================
    // 2. NEW RELEASES CHANNEL
    // =========================================================================

    @Test
    @DisplayName("getNewReleasesPoolElements & getNewReleasesSeriesIds")
    void newReleasesChannel() {
        when(listOperations.range("pool:new_releases", 0, -1)).thenReturn(List.of("nr1"));
        assertThat(channelService.getNewReleasesPoolElements()).containsExactly("nr1");

        assertThat(channelService.getNewReleasesSeriesIds("u1", 0)).isEmpty();
        when(listOperations.size("pool:new_releases")).thenReturn(1L);
        when(valueOperations.get("offset:new_releases:u1")).thenReturn("0");
        when(listOperations.range("pool:new_releases", 0L, 0L)).thenReturn(List.of("nr1"));
        assertThat(channelService.getNewReleasesSeriesIds("u1", 1)).containsExactly("nr1");
    }

    @Test
    @DisplayName("refreshNewReleasesPool")
    void refreshNewReleasesPool() {
        when(listOperations.range("pool:new_releases", 0, -1)).thenReturn(List.of("nrOld"));
        TrendingSampleConfigRes cfg = new TrendingSampleConfigRes();
        cfg.setMaxImpression(500L);
        when(trendingSampleConfigService.getConfig()).thenReturn(cfg);

        when(seriesRepository.findCandidateNewReleasesSeriesIds(
                eq(SeriesStatus.PUBLISHED), eq(500L), anySet(), eq(false), eq(ImpressionStatus.ON_GOING), any(Pageable.class)
        )).thenReturn(List.of("nrNew"));

        List<String> res = channelService.refreshNewReleasesPool(List.of("bl1"), 2);
        assertThat(res).containsExactly("nrOld", "nrNew");
    }

    // =========================================================================
    // 3. RECENTLY UPDATED CHANNEL
    // =========================================================================

    @Test
    @DisplayName("getRecentlyUpdatedPoolElements & getRecentlyUpdatedSeriesIds & refresh")
    void recentlyUpdatedChannel() {
        when(listOperations.range("pool:recently_updated", 0, -1)).thenReturn(List.of("ru1"));
        assertThat(channelService.getRecentlyUpdatedPoolElements()).containsExactly("ru1");

        assertThat(channelService.getRecentlyUpdatedSeriesIds("u1", -1)).isEmpty();
        when(listOperations.size("pool:recently_updated")).thenReturn(1L);
        when(valueOperations.get("offset:recently_updated:u1")).thenReturn("0");
        when(listOperations.range("pool:recently_updated", 0L, 0L)).thenReturn(List.of("ru1"));
        assertThat(channelService.getRecentlyUpdatedSeriesIds("u1", 1)).containsExactly("ru1");

        when(seriesRepository.findCandidateRecentlyUpdatedSeriesIds(
                eq(SeriesStatus.PUBLISHED), anySet(), eq(false), any(Pageable.class)
        )).thenReturn(List.of("ruNew"));

        List<String> res = channelService.refreshRecentlyUpdatedPool(null, 2);
        assertThat(res).containsExactly("ru1", "ruNew");
    }

    // =========================================================================
    // 4. LATEST COMMUNITY CHOICE CHANNEL
    // =========================================================================

    @Test
    @DisplayName("latestCommunityChoiceChannel")
    void latestCommunityChoiceChannel() {
        when(listOperations.range("pool:latest_community_choice", 0, -1)).thenReturn(List.of("lcc1"));
        assertThat(channelService.getLatestCommunityChoicePoolElements()).containsExactly("lcc1");

        assertThat(channelService.getLatestCommunityChoiceSeriesIds("u1", 0)).isEmpty();
        when(listOperations.size("pool:latest_community_choice")).thenReturn(1L);
        when(valueOperations.get("offset:latest_community_choice:u1")).thenReturn("0");
        when(listOperations.range("pool:latest_community_choice", 0L, 0L)).thenReturn(List.of("lcc1"));
        assertThat(channelService.getLatestCommunityChoiceSeriesIds("u1", 1)).containsExactly("lcc1");

        when(seriesLogRepository.findCandidateTrendingSeriesIds(
                eq(SeriesStatus.PUBLISHED.name()), any(), anySet(), eq(false), eq(2)
        )).thenReturn(List.of("lccNew"));

        List<String> res = channelService.refreshLatestCommunityChoicePool(List.of("b1"), 2);
        assertThat(res).containsExactly("lcc1", "lccNew");
    }

    // =========================================================================
    // 5. COMMUNITY CHOICE CHANNEL
    // =========================================================================

    @Test
    @DisplayName("communityChoiceChannel")
    void communityChoiceChannel() {
        when(listOperations.range("pool:community_choice", 0, -1)).thenReturn(List.of("cc1"));
        assertThat(channelService.getCommunityChoicePoolElements()).containsExactly("cc1");

        assertThat(channelService.getCommunityChoiceSeriesIds("u1", 0)).isEmpty();
        when(listOperations.size("pool:community_choice")).thenReturn(1L);
        when(valueOperations.get("offset:community_choice:u1")).thenReturn("0");
        when(listOperations.range("pool:community_choice", 0L, 0L)).thenReturn(List.of("cc1"));
        assertThat(channelService.getCommunityChoiceSeriesIds("u1", 1)).containsExactly("cc1");

        when(seriesRepository.findCandidateCommunityChoiceSeriesIds(
                eq(SeriesStatus.PUBLISHED), anySet(), eq(false), any(Pageable.class)
        )).thenReturn(List.of("ccNew"));

        List<String> res = channelService.refreshCommunityChoicePool(null, 2);
        assertThat(res).containsExactly("cc1", "ccNew");
    }

    // =========================================================================
    // 6. RANDOM CATEGORY CHANNEL
    // =========================================================================

    @Test
    @DisplayName("randomCategoryChannel")
    void randomCategoryChannel() {
        when(listOperations.range("pool:random_category", 0, -1)).thenReturn(List.of("rc1"));
        assertThat(channelService.getRandomCategoryPoolElements()).containsExactly("rc1");

        assertThat(channelService.getRandomCategorySeriesIds("u1", 0)).isEmpty();
        when(listOperations.size("pool:random_category")).thenReturn(1L);
        when(valueOperations.get("offset:random_category:u1")).thenReturn("0");
        when(listOperations.range("pool:random_category", 0L, 0L)).thenReturn(List.of("rc1"));
        assertThat(channelService.getRandomCategorySeriesIds("u1", 1)).containsExactly("rc1");

        when(seriesRepository.findTopSeriesPerCategory(
                eq(SeriesStatus.PUBLISHED.name()), eq("ACTIVE"), anySet(), eq(false), eq(3)
        )).thenReturn(List.of("rcNew"));

        List<String> res = channelService.refreshRandomCategoryPool(List.of("b1"), 3, 2);
        assertThat(res).containsExactly("rc1", "rcNew");
    }

    // =========================================================================
    // 7. SUBSCRIBED CREATORS CHANNEL
    // =========================================================================

    @Test
    @DisplayName("getSubscribedCreatorsSeriesIds & refreshSubscribedCreatorsPool & getAllSubscribedCreatorsSeriesIds")
    void subscribedCreatorsChannel() {
        assertThat(channelService.getSubscribedCreatorsSeriesIds(null, Set.of(), 5)).isEmpty();
        assertThat(channelService.getSubscribedCreatorsSeriesIds("  ", Set.of(), 5)).isEmpty();
        assertThat(channelService.getSubscribedCreatorsSeriesIds("u1", Set.of(), 0)).isEmpty();

        // Stub range(..., 0, -1) TRƯỚC KHI gọi refreshSubscribedCreatorsPool:
        // Lần 1 (trong refreshSubscribedCreatorsPool) trả về null
        // Lần 2 (trong getAllSubscribedCreatorsSeriesIds) trả về List.of("sub1")
        when(listOperations.range("pool:subscribed_creators:u1", 0, -1))
                .thenReturn(null)
                .thenReturn(List.of("sub1"));

        // Pool empty -> triggers refreshSubscribedCreatorsPool
        when(listOperations.size("pool:subscribed_creators:u1")).thenReturn(null);
        when(seriesRepository.findTopSeriesFromFollowedCreators(eq("u1"), eq("PUBLISHED"), anySet(), eq(true), eq(1)))
                .thenReturn(List.of("sub1", "sub2"));

        when(valueOperations.get("offset:subscribed_creators:u1")).thenReturn("0");
        when(listOperations.range("pool:subscribed_creators:u1", 0L, 1L)).thenReturn(List.of("sub1", "sub2"));

        List<String> res = channelService.getSubscribedCreatorsSeriesIds("u1", Set.of(), 2);
        assertThat(res).containsExactly("sub1", "sub2");

        // refresh null account
        assertThat(channelService.refreshSubscribedCreatorsPool("", Set.of(), 1, 10)).isEmpty();

        // getAllSubscribedCreatorsSeriesIds
        assertThat(channelService.getAllSubscribedCreatorsSeriesIds("")).isEmpty();
        assertThat(channelService.getAllSubscribedCreatorsSeriesIds("u1")).containsExactly("sub1");
    }

    // =========================================================================
    // 8. TRENDING CHANNEL
    // =========================================================================

    @Test
    @DisplayName("trendingChannel")
    void trendingChannel() {
        when(listOperations.range("pool:trending", 0, -1)).thenReturn(List.of("t1"));
        assertThat(channelService.getTrendingPoolElements()).containsExactly("t1");

        assertThat(channelService.getTrendingSeriesIds("u1", 0)).isEmpty();
        when(listOperations.size("pool:trending")).thenReturn(1L);
        when(valueOperations.get("offset:trending:u1")).thenReturn("0");
        when(listOperations.range("pool:trending", 0L, 0L)).thenReturn(List.of("t1"));
        assertThat(channelService.getTrendingSeriesIds("u1", 1)).containsExactly("t1");

        when(seriesRepository.findCandidateTrendingSeriesIds(
                eq(SeriesStatus.PUBLISHED), eq(ImpressionStatus.SUCCESS), anySet(), eq(false), any(Pageable.class)
        )).thenReturn(List.of("tNew"));

        List<String> res = channelService.refreshTrendingPool(List.of("b1"), 2);
        assertThat(res).containsExactly("t1", "tNew");
    }

    // =========================================================================
    // 9. ONBOARDING PREFERENCES CHANNEL
    // =========================================================================

    @Test
    @DisplayName("getOnboardingPreferencesSeriesIds - Various scenarios")
    void getOnboardingPreferencesSeriesIds() {
        assertThat(channelService.getOnboardingPreferencesSeriesIds(null, null, 10)).isEmpty();
        assertThat(channelService.getOnboardingPreferencesSeriesIds("  ", null, 10)).isEmpty();

        // Static feature null
        when(userFeatureService.getUserStaticFeatureByAccountId("u1")).thenReturn(null);
        assertThat(channelService.getOnboardingPreferencesSeriesIds("u1", null, 10)).isEmpty();

        // Static feature empty genres & tags
        UserStaticFeature featureEmpty = new UserStaticFeature();
        when(userFeatureService.getUserStaticFeatureByAccountId("u1")).thenReturn(featureEmpty);
        assertThat(channelService.getOnboardingPreferencesSeriesIds("u1", null, 10)).isEmpty();

        // Static feature with genres and tags
        UserStaticFeature featureWithData = new UserStaticFeature();
        featureWithData.setOnboardingGenres(List.of("Action"));
        featureWithData.setOnboardingTags(List.of("Superhero"));
        when(userFeatureService.getUserStaticFeatureByAccountId("u1")).thenReturn(featureWithData);

        when(seriesRepository.findCandidateSeriesByGenresAndTags(
                eq(SeriesStatus.PUBLISHED), eq(List.of("Action")), eq(true), eq(List.of("Superhero")), eq(true), anySet(), eq(false), any(Pageable.class)
        )).thenReturn(List.of("s1", "s2"));

        List<String> res = channelService.getOnboardingPreferencesSeriesIds("u1", Set.of("bl1"), 10);
        assertThat(res).containsExactlyInAnyOrder("s1", "s2");
    }

    // =========================================================================
    // 10. DYNAMIC PREFERENCES CHANNEL
    // =========================================================================

    @Test
    @DisplayName("getDynamicPreferencesSeriesIds - Various scenarios")
    void getDynamicPreferencesSeriesIds() {
        assertThat(channelService.getDynamicPreferencesSeriesIds(null, null, 10)).isEmpty();
        assertThat(channelService.getDynamicPreferencesSeriesIds("u1", null, 0)).isEmpty();

        // Dynamic feature null
        when(userFeatureService.getUserDynamicFeatureByAccountId("u1")).thenReturn(null);
        assertThat(channelService.getDynamicPreferencesSeriesIds("u1", null, 10)).isEmpty();

        // Dynamic feature empty
        UserDynamicFeature featureEmpty = new UserDynamicFeature();
        when(userFeatureService.getUserDynamicFeatureByAccountId("u1")).thenReturn(featureEmpty);
        assertThat(channelService.getDynamicPreferencesSeriesIds("u1", null, 10)).isEmpty();

        // Dynamic feature with categories & tags
        UserDynamicFeature featureWithData = new UserDynamicFeature();
        featureWithData.setCategories(List.of("Drama"));
        featureWithData.setTags(List.of("Romance"));
        when(userFeatureService.getUserDynamicFeatureByAccountId("u1")).thenReturn(featureWithData);

        when(seriesRepository.findCandidateSeriesByGenresAndTags(
                eq(SeriesStatus.PUBLISHED), eq(List.of("Drama")), eq(true), eq(List.of("Romance")), eq(true), anySet(), eq(true), any(Pageable.class)
        )).thenReturn(List.of("d1", "d2"));

        List<String> res = channelService.getDynamicPreferencesSeriesIds("u1", null, 10);
        assertThat(res).containsExactlyInAnyOrder("d1", "d2");
    }

    // =========================================================================
    // 11. GLOBAL IDS
    // =========================================================================

    @Test
    @DisplayName("getAllGlobalIds & updateGlobalIds")
    void globalIds() {
        when(setOperations.members("recommendation:global_ids")).thenReturn(Set.of("g1", "g2")).thenReturn(null);
        assertThat(channelService.getAllGlobalIds()).containsExactlyInAnyOrder("g1", "g2");
        assertThat(channelService.getAllGlobalIds()).isEmpty();

        // updateGlobalIds null or empty -> deletes
        channelService.updateGlobalIds(null);
        verify(redisTemplate).delete("recommendation:global_ids");

        channelService.updateGlobalIds(Collections.emptySet());
        verify(redisTemplate, times(2)).delete("recommendation:global_ids");

        // updateGlobalIds populated
        channelService.updateGlobalIds(Set.of("g1", "g2"));
        verify(setOperations).add(eq("recommendation:global_ids"), any(String[].class));
        verify(redisTemplate).expire(eq("recommendation:global_ids"), any());

        // Exception in updateGlobalIds
        doThrow(new RuntimeException("Redis error")).when(redisTemplate).delete("recommendation:global_ids");
        channelService.updateGlobalIds(Set.of("g1")); // should log error without throwing
    }
}
