package com.talex.server.services.recommend.impls;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.talex.server.dtos.mongo.UserStaticFeature;
import com.talex.server.dtos.recommend.request.HomeFeedRequestDto;
import com.talex.server.dtos.recommend.response.*;
import com.talex.server.entities.mongo.SeriesRecommendation;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.repositories.mongo.SeriesRecommendationRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.services.mongo.UserFeatureService;
import com.talex.server.services.recommend.SeriesChannelService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.kafka.core.KafkaTemplate;

import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RecommendationServiceImpl Tests")
class RecommendationServiceImplTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private SeriesRecommendationRepository seriesRecommendationRepository;
    @Mock
    private SeriesChannelService seriesChannelService;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private JdbcTemplate questDbJdbcTemplate;
    @Mock
    private KafkaTemplate<String, String> kafkaTemplate;
    @Mock
    private ObjectMapper objectMapper;
    @Mock
    private UserFeatureService userFeatureService;

    @Mock
    private ListOperations<String, String> listOperations;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private SetOperations<String, String> setOperations;

    private RecommendationServiceImpl recommendationService;
    private final String pythonApi = "http://localhost:8000";

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForList()).thenReturn(listOperations);
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(redisTemplate.opsForSet()).thenReturn(setOperations);

        recommendationService = new RecommendationServiceImpl(
                pythonApi, redisTemplate, objectMapper, kafkaTemplate,
                questDbJdbcTemplate, seriesRecommendationRepository,
                seriesChannelService, seriesRepository, userFeatureService
        );
    }

    // =========================================================================
    // 1. GET HOME FEED SERIES
    // =========================================================================

    @Test
    @DisplayName("getHomeFeedSeries - Empty unique IDs case")
    void getHomeFeedSeries_EmptyUniqueIds() {
        HomeFeedRequestDto request = new HomeFeedRequestDto();
        when(seriesChannelService.getAllGlobalIds()).thenReturn(Set.of("g1"));
        when(seriesChannelService.getPromotedSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getTrendingSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getNewReleasesSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRecentlyUpdatedSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getLatestCommunityChoiceSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getCommunityChoiceSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRandomCategorySeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getSubscribedCreatorsSeriesIds(anyString(), anySet(), anyInt())).thenReturn(Collections.emptyList());

        HomePoolsSeriesResponseDto response = recommendationService.getHomeFeedSeries("acc-1", request);

        assertThat(response).isNotNull();
        assertThat(response.getPromoted()).isEmpty();
        assertThat(response.getTrending()).isEmpty();
    }

    @Test
    @DisplayName("getHomeFeedSeries - Successful retrieval with null request & valid user")
    void getHomeFeedSeries_Success() {
        when(seriesChannelService.getAllGlobalIds()).thenReturn(Set.of("g1"));
        when(seriesChannelService.getPromotedSeriesIds(eq("acc-1"), anyInt())).thenReturn(List.of("s1"));
        when(seriesChannelService.getTrendingSeriesIds(eq("acc-1"), anyInt())).thenReturn(List.of("s2"));
        when(seriesChannelService.getNewReleasesSeriesIds(eq("acc-1"), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRecentlyUpdatedSeriesIds(eq("acc-1"), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getLatestCommunityChoiceSeriesIds(eq("acc-1"), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getCommunityChoiceSeriesIds(eq("acc-1"), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRandomCategorySeriesIds(eq("acc-1"), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getSubscribedCreatorsSeriesIds(eq("acc-1"), anySet(), anyInt())).thenReturn(Collections.emptyList());

        SeriesCardResponseDto card1 = SeriesCardResponseDto.builder().seriesId("s1").title("Series 1").build();
        SeriesCardResponseDto card2 = SeriesCardResponseDto.builder().seriesId("s2").title("Series 2").build();

        when(seriesRepository.findSeriesCardsByIds(anySet(), eq(SeriesStatus.PUBLISHED)))
                .thenReturn(List.of(card1, card2));

        HomePoolsSeriesResponseDto response = recommendationService.getHomeFeedSeries("acc-1", null);

        assertThat(response).isNotNull();
        assertThat(response.getPromoted()).hasSize(1);
        assertThat(response.getPromoted().get(0).getSeriesId()).isEqualTo("s1");
        assertThat(response.getTrending()).hasSize(1);
        assertThat(response.getTrending().get(0).getSeriesId()).isEqualTo("s2");

        // Verify Kafka impression fired for valid account
        verify(kafkaTemplate, timeout(1000)).send(eq("home-impression-log-topic"), eq("acc-1"), any());
    }

    @Test
    @DisplayName("getHomeFeedSeries - Exception handling in async tasks")
    void getHomeFeedSeries_AsyncExceptionsHandled() {
        when(seriesChannelService.getAllGlobalIds()).thenThrow(new RuntimeException("Global error"));
        when(seriesChannelService.getPromotedSeriesIds(anyString(), anyInt())).thenThrow(new RuntimeException("Promoted error"));
        when(seriesChannelService.getTrendingSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getNewReleasesSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRecentlyUpdatedSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getLatestCommunityChoiceSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getCommunityChoiceSeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRandomCategorySeriesIds(anyString(), anyInt())).thenReturn(Collections.emptyList());

        HomePoolsSeriesResponseDto response = recommendationService.getHomeFeedSeries("guest_user", new HomeFeedRequestDto());

        assertThat(response).isNotNull();
        assertThat(response.getPromoted()).isEmpty();
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }

    // =========================================================================
    // 2. PERSONALIZED RECOMMENDATIONS & POOL INIT
    // =========================================================================

    @Test
    @DisplayName("getPersonalizedRecommendations - Pool exists, normal range read")
    void getPersonalizedRecommendations_PoolExists() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(valueOperations.get(contains("recommendation:offset"))).thenReturn("0");

        // Thay eq(0), eq(11) bằng eq(0L), eq(11L) để khớp kiểu long của ListOperations.range
        when(listOperations.range(contains("recommendation:pool"), eq(0L), eq(11L)))
                .thenReturn(List.of("s1:0.9500", "s2:another_channel", "s3:null"));

        SeriesCardResponseDto card1 = SeriesCardResponseDto.builder().seriesId("s1").build();
        SeriesCardResponseDto card2 = SeriesCardResponseDto.builder().seriesId("s2").build();
        SeriesCardResponseDto card3 = SeriesCardResponseDto.builder().seriesId("s3").build();

        when(seriesRepository.findSeriesCardsByIds(anySet(), eq(SeriesStatus.PUBLISHED)))
                .thenReturn(List.of(card1, card2, card3));

        List<SeriesCardResponseDto> res = recommendationService.getPersonalizedRecommendations("acc-1", "sess-1", "HOME", 12);

        assertThat(res).extracting(SeriesCardResponseDto::getSeriesId).containsExactly("s1", "s2", "s3");
        verify(valueOperations).set(contains("recommendation:offset"), eq("3"), any());
        verify(kafkaTemplate, timeout(1000)).send(eq("home-impression-log-topic"), eq("acc-1"), any());
        verify(setOperations).add(contains("recommendation:already_watched:acc-1"), any(String[].class));
    }

    @Test
    @DisplayName("getPersonalizedRecommendations - Empty raw items returned from Redis")
    void getPersonalizedRecommendations_EmptyRawItems() {
        when(redisTemplate.hasKey(anyString())).thenReturn(true);
        when(valueOperations.get(contains("recommendation:offset"))).thenReturn(null);
        when(listOperations.range(anyString(), anyLong(), anyLong())).thenReturn(Collections.emptyList());

        List<SeriesCardResponseDto> res = recommendationService.getPersonalizedRecommendations("", "", "HOME", -1);

        assertThat(res).isEmpty();
    }

    @Test
    @DisplayName("getPersonalizedRecommendations - Pool absent, triggers initialization (User with no watch history)")
    void getPersonalizedRecommendations_InitPool_NoHistory() {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);
        when(valueOperations.get(contains("recommendation:offset"))).thenReturn("0");

        // Mock 1. Lấy lịch sử xem gần đây (Empty đối với trường hợp No History)
        when(listOperations.range(contains("watch:top5:recent_series"), eq(0L), eq(4L)))
                .thenReturn(Collections.emptyList());

        // Mock 16 channel IDs
        when(seriesChannelService.getPromotedSeriesIds(anyString(), eq(2))).thenReturn(List.of("c1", "c2"));
        when(seriesChannelService.getTrendingSeriesIds(anyString(), eq(2))).thenReturn(List.of("c3"));
        when(seriesChannelService.getNewReleasesSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRecentlyUpdatedSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getLatestCommunityChoiceSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getCommunityChoiceSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRandomCategorySeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getAllGlobalIds()).thenReturn(Set.of("g1"));
        when(seriesChannelService.getSubscribedCreatorsSeriesIds(anyString(), anySet(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getAllSubscribedCreatorsSeriesIds(anyString())).thenReturn(List.of());

        // QuestDB returns empty recent series
        when(questDbJdbcTemplate.query(anyString(), any(RowMapper.class), anyString())).thenReturn(Collections.emptyList());

        // Onboarding candidates
        when(seriesChannelService.getOnboardingPreferencesSeriesIds(eq("acc-1"), anySet(), eq(50)))
                .thenReturn(List.of("on1", "on2"));

        // Mock 2. Đọc danh sách từ recommendation pool vừa khởi tạo
        when(listOperations.range(contains("recommendation:pool"), eq(0L), eq(11L)))
                .thenReturn(List.of("on1:0.8000", "on2:0.7000", "c1:another_channel"));

        SeriesCardResponseDto card1 = SeriesCardResponseDto.builder().seriesId("on1").build();
        SeriesCardResponseDto card2 = SeriesCardResponseDto.builder().seriesId("on2").build();
        SeriesCardResponseDto card3 = SeriesCardResponseDto.builder().seriesId("c1").build();

        when(seriesRepository.findSeriesCardsByIds(anySet(), eq(SeriesStatus.PUBLISHED)))
                .thenReturn(List.of(card1, card2, card3));

        List<SeriesCardResponseDto> res = recommendationService.getPersonalizedRecommendations("acc-1", "sess-1", "HOME", 12);

        assertThat(res).hasSize(3);
        verify(listOperations).rightPushAll(contains("recommendation:pool"), anyList());
    }

    @Test
    @DisplayName("getPersonalizedRecommendations - Init pool with watch history and DETAIL pageType fallback")
    void getPersonalizedRecommendations_InitPool_WithHistory_Detail() throws Exception {
        when(redisTemplate.hasKey(anyString())).thenReturn(false);

        // 16 channel IDs
        when(seriesChannelService.getPromotedSeriesIds(anyString(), eq(2))).thenReturn(List.of("c1"));
        when(seriesChannelService.getTrendingSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getNewReleasesSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRecentlyUpdatedSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getLatestCommunityChoiceSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getCommunityChoiceSeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getRandomCategorySeriesIds(anyString(), eq(2))).thenReturn(Collections.emptyList());
        when(seriesChannelService.getAllGlobalIds()).thenReturn(Set.of());
        when(seriesChannelService.getSubscribedCreatorsSeriesIds(anyString(), anySet(), eq(2))).thenReturn(Collections.emptyList());

        // Cache hit for getRecentWatchedSeries (Dùng eq(0L), eq(4L) thay vì eq(0), eq(4))
        when(listOperations.range(contains("watch:top5:recent_series"), eq(0L), eq(4L))).thenReturn(List.of("rec1"));

        // Similar series from MongoDB
        SeriesRecommendation rec = new SeriesRecommendation();
        rec.setSimilarIds(List.of("sim1", "c1")); // c1 is in channel16
        when(seriesRecommendationRepository.findById("rec1")).thenReturn(Optional.of(rec));
        when(objectMapper.writeValueAsString(any())).thenReturn("[\"sim1\",\"c1\"]");

        // User account older than 1 hour -> triggers dynamic preferences fallback
        UserStaticFeature staticFeature = new UserStaticFeature();
        staticFeature.setCreatedAt(LocalDateTime.now().minusHours(2));
        when(userFeatureService.getUserStaticFeatureByAccountId("acc-1")).thenReturn(staticFeature);

        when(seriesChannelService.getDynamicPreferencesSeriesIds(eq("acc-1"), anySet(), anyInt()))
                .thenReturn(List.of("dyn1"));

        // Dùng eq(0L), eq(11L) thay vì eq(0), eq(11)
        when(listOperations.range(contains("recommendation:pool"), eq(0L), eq(11L)))
                .thenReturn(List.of("sim1:null"));
        when(seriesRepository.findSeriesCardsByIds(anySet(), eq(SeriesStatus.PUBLISHED)))
                .thenReturn(List.of(SeriesCardResponseDto.builder().seriesId("sim1").build()));

        List<SeriesCardResponseDto> res = recommendationService.getPersonalizedRecommendations("acc-1", "sess-1", "DETAIL", 12);

        assertThat(res).hasSize(1);
    }

    // =========================================================================
    // 3. GET LATEST RECOMMENDATION POOL SERIES
    // =========================================================================

    @Test
    @DisplayName("getLatestRecommendationPoolSeries - Account null/empty returns empty")
    void getLatestRecommendationPoolSeries_NullAccount() {
        assertThat(recommendationService.getLatestRecommendationPoolSeries(null, "s1", "HOME")).isEmpty();
        assertThat(recommendationService.getLatestRecommendationPoolSeries("  ", "s1", "HOME")).isEmpty();
    }

    @Test
    @DisplayName("getLatestRecommendationPoolSeries - Null sessionId searches pattern in Redis")
    void getLatestRecommendationPoolSeries_SearchSessionId() {
        String keyOld = "recommendation:pool:acc-1:sess99:HOME";
        String keyLatest = "recommendation:pool:acc-1:sess100:HOME";

        when(redisTemplate.keys("recommendation:pool:acc-1:*:HOME"))
                .thenReturn(Set.of(keyOld, keyLatest));
        when(redisTemplate.getExpire(keyOld)).thenReturn(1000L);
        when(redisTemplate.getExpire(keyLatest)).thenReturn(3600L);

        when(listOperations.range(keyLatest, 0, -1))
                .thenReturn(List.of("s1:0.9999", "s2"));

        SeriesCardResponseDto card1 = SeriesCardResponseDto.builder().seriesId("s1").build();
        SeriesCardResponseDto card2 = SeriesCardResponseDto.builder().seriesId("s2").build();
        when(seriesRepository.findSeriesCardsByIds(anySet(), eq(SeriesStatus.PUBLISHED)))
                .thenReturn(List.of(card1, card2));

        List<PoolSeriesCardResponseDto> res = recommendationService.getLatestRecommendationPoolSeries("acc-1", null, null);

        assertThat(res).hasSize(2);
        assertThat(res.get(0).getScore()).isEqualTo("0.9999");
        assertThat(res.get(1).getScore()).isEqualTo("null");
    }

    @Test
    @DisplayName("getLatestRecommendationPoolSeries - Empty pool items or missing keys")
    void getLatestRecommendationPoolSeries_EmptyPool() {
        when(redisTemplate.keys(anyString())).thenReturn(Collections.emptySet());
        assertThat(recommendationService.getLatestRecommendationPoolSeries("acc-1", null, "HOME")).isEmpty();

        when(listOperations.range("recommendation:pool:acc-1:sess-1:HOME", 0, -1)).thenReturn(null);
        assertThat(recommendationService.getLatestRecommendationPoolSeries("acc-1", "sess-1", "HOME")).isEmpty();
    }

    // =========================================================================
    // 4. ALREADY WATCHED POOL
    // =========================================================================

    @Test
    @DisplayName("getAlreadyWatchedPoolSeries - Various cases")
    void getAlreadyWatchedPoolSeries() {
        assertThat(recommendationService.getAlreadyWatchedPoolSeries("")).isEmpty();

        when(setOperations.members("recommendation:already_watched:acc-1")).thenReturn(null).thenReturn(Set.of("s1"));
        assertThat(recommendationService.getAlreadyWatchedPoolSeries("acc-1")).isEmpty();

        SeriesCardResponseDto card1 = SeriesCardResponseDto.builder().seriesId("s1").build();
        when(seriesRepository.findSeriesCardsByIds(anySet(), eq(SeriesStatus.PUBLISHED))).thenReturn(List.of(card1));

        List<SeriesCardResponseDto> res = recommendationService.getAlreadyWatchedPoolSeries("acc-1");
        assertThat(res).containsExactly(card1);
    }

    // =========================================================================
    // 5. RECENT WATCHED SERIES
    // =========================================================================

    @Test
    @DisplayName("getRecentWatchedSeries - Cache hit vs Cache miss & QuestDB")
    void getRecentWatchedSeries() {
        assertThat(recommendationService.getRecentWatchedSeries(null)).isEmpty();

        // Cache hit
        when(listOperations.range("watch:top5:recent_series:acc-1", 0, 4)).thenReturn(List.of("s1", "s2"));
        assertThat(recommendationService.getRecentWatchedSeries("acc-1")).containsExactly("s1", "s2");

        // Cache miss -> QuestDB query
        when(listOperations.range("watch:top5:recent_series:acc-2", 0, 4)).thenReturn(Collections.emptyList());
        when(questDbJdbcTemplate.query(anyString(), any(RowMapper.class), eq("acc-2"))).thenReturn(List.of("s3"));

        List<String> res = recommendationService.getRecentWatchedSeries("acc-2");
        assertThat(res).containsExactly("s3");
        verify(redisTemplate).delete("watch:top5:recent_series:acc-2");
        verify(listOperations).rightPushAll("watch:top5:recent_series:acc-2", List.of("s3"));

        // QuestDB Exception handling
        when(questDbJdbcTemplate.query(anyString(), any(RowMapper.class), eq("acc-3")))
                .thenThrow(new RuntimeException("QuestDB offline"));
        assertThat(recommendationService.getRecentWatchedSeries("acc-3")).isEmpty();
    }

    // =========================================================================
    // 6. SIMILAR SERIES IDS
    // =========================================================================

    @Test
    @DisplayName("getSimilarSeriesIds - Redis cache hit vs MongoDB lookup & Error handling")
    void getSimilarSeriesIds() throws Exception {
        assertThat(recommendationService.getSimilarSeriesIds("")).isEmpty();

        // Redis Cache Hit
        when(valueOperations.get("recommendation:series:s1")).thenReturn("[\"sim1\",\"sim2\"]");
        when(objectMapper.readValue(eq("[\"sim1\",\"sim2\"]"), any(TypeReference.class))).thenReturn(List.of("sim1", "sim2"));

        List<String> res1 = recommendationService.getSimilarSeriesIds("s1");
        assertThat(res1).containsExactly("sim1", "sim2");

        // Redis Cache Miss -> MongoDB hit
        when(valueOperations.get("recommendation:series:s2")).thenReturn(null);
        SeriesRecommendation rec = new SeriesRecommendation();
        rec.setSimilarIds(List.of("sim3"));
        when(seriesRecommendationRepository.findById("s2")).thenReturn(Optional.of(rec));
        when(objectMapper.writeValueAsString(List.of("sim3"))).thenReturn("[\"sim3\"]");

        List<String> res2 = recommendationService.getSimilarSeriesIds("s2");
        assertThat(res2).containsExactly("sim3");
        verify(valueOperations).set("recommendation:series:s2", "[\"sim3\"]", java.time.Duration.ofDays(7));

        // MongoDB empty similarIds or not found
        when(seriesRecommendationRepository.findById("s3")).thenReturn(Optional.empty());
        assertThat(recommendationService.getSimilarSeriesIds("s3")).isEmpty();

        // JSON parse error
        when(valueOperations.get("recommendation:series:s4")).thenReturn("bad json");
        when(objectMapper.readValue(eq("bad json"), any(TypeReference.class))).thenThrow(new RuntimeException("JSON error"));
        assertThat(recommendationService.getSimilarSeriesIds("s4")).isEmpty();
    }

    // =========================================================================
    // 7. RANK SERIES
    // =========================================================================

    @Test
    @DisplayName("rankSeries - Invalid parameters or Python API fallback")
    void rankSeries() {
        assertThat(recommendationService.rankSeries("acc-1", null)).isEmpty();
        assertThat(recommendationService.rankSeries("acc-1", Collections.emptyList())).isEmpty();
        assertThat(recommendationService.rankSeries(null, List.of("s1"))).isEmpty();

        // HTTP call error fallback
        List<RankResultItem> res = recommendationService.rankSeries("acc-1", List.of("s1"));
        assertThat(res).isEmpty();
    }

    // =========================================================================
    // 8. SEND HOME IMPRESSIONS ASYNC
    // =========================================================================

    @Test
    @DisplayName("sendHomeImpressionsAsync - Exception handling")
    void sendHomeImpressionsAsync_ExceptionHandled() throws Exception {
        when(objectMapper.writeValueAsString(any())).thenThrow(new RuntimeException("Jackson error"));
        recommendationService.sendHomeImpressionsAsync("acc-1", List.of("s1"));
        verify(kafkaTemplate, never()).send(anyString(), anyString(), anyString());
    }
}
