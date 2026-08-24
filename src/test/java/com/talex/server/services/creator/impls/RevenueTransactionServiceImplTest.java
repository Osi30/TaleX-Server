package com.talex.server.services.creator.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.responses.creator.CreatorResponseDto;
import com.talex.server.dtos.responses.creator.CreatorTierResponseDto;
import com.talex.server.dtos.revenue.response.RevenueSummaryResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTimeSeriesResponseDto;
import com.talex.server.dtos.revenue.response.RevenueTransactionDto;
import com.talex.server.dtos.settlement.episode.TotalEpisodeRevenueDto;
import com.talex.server.dtos.settlement.series.TotalSeriesRevenueDto;
import com.talex.server.entities.config.CreatorConfig;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.creator.RevenueTransaction;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.EpisodeUnlockedContent;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.creator.RevenueTransactionType;
import com.talex.server.mappers.settlement.RevenueTransactionMapper;
import com.talex.server.repositories.series.ComboEpisodeRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.SeriesRepository;
import com.talex.server.repositories.subscription.SubscriptionRevenueLogRepository;
import com.talex.server.repositories.transaction.RevenueTransactionRepository;
import com.talex.server.services.config.CreatorConfigService;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.series.EpisodeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("RevenueTransactionServiceImpl Tests")
class RevenueTransactionServiceImplTest {

    @Mock
    private RevenueTransactionMapper revenueTransactionMapper;
    @Mock
    private RevenueTransactionRepository revenueTransactionRepository;
    @Mock
    private EpisodeService episodeService;
    @Mock
    private CreatorService creatorService;
    @Mock
    private CreatorConfigService creatorConfigService;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private ComboEpisodeRepository comboEpisodeRepository;
    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private SubscriptionRevenueLogRepository subscriptionRevenueLogRepository;

    @InjectMocks
    private RevenueTransactionServiceImpl service;

    private Creator sampleCreator;
    private CreatorResponseDto sampleCreatorDto;
    private CreatorTierResponseDto sampleTierDto;
    private CreatorConfig sampleConfig;

    @BeforeEach
    void setUp() {
        sampleCreator = Creator.builder().creatorId("creator-1").build();
        sampleTierDto = CreatorTierResponseDto.builder().tierLevel(1).directPurchaseShareRatio(0.2).build();
        sampleCreatorDto = CreatorResponseDto.builder().creatorId("creator-1").currentBalance(BigDecimal.valueOf(100000)).creatorTier(sampleTierDto).build();
        sampleConfig = CreatorConfig.builder().baseUnlockShare(0.7).build();
    }

    // =========================================================================
    // 1. createFromEpisodeOrder
    // =========================================================================

    @Test
    @DisplayName("createFromEpisodeOrder - Episode order with single unlocked content")
    void createFromEpisodeOrder_SingleEpisode() {
        Order order = Order.builder()
                .orderId("order-100")
                .itemType("EPISODE")
                .itemId("ep-1")
                .totalAmount(BigDecimal.valueOf(10000))
                .vatAmount(BigDecimal.valueOf(1000))
                .build();

        Series series = new Series();
        series.setTitle("My Series");
        Season season = new Season();
        season.setSeries(series);
        Episode ep = new Episode();
        ep.setEpisodeNumber(1);
        ep.setSeason(season);
        EpisodeUnlockedContent content = EpisodeUnlockedContent.builder().episode(ep).build();

        when(episodeService.getCreatorIdByEpisodeId("ep-1")).thenReturn("creator-1");
        when(creatorService.getById("creator-1")).thenReturn(sampleCreatorDto);
        when(creatorConfigService.getConfigEntity()).thenReturn(sampleConfig);
        when(creatorService.getEntityById("creator-1")).thenReturn(sampleCreator);
        when(revenueTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RevenueTransaction tx = service.createFromEpisodeOrder(order, List.of(content));

        assertThat(tx).isNotNull();
        assertThat(tx.getReferenceId()).isEqualTo("order-100");

        // Net amount = 10,000 - 1,000 = 9,000.
        // baseUnlockShare = 0.7, directPurchaseShareRatio = 0.2 -> platformShareRatio = 0.5.
        // platformAmount = 9000 * 0.5 = 4500. creatorAmount = 9000 - 4500 = 4500.
        assertThat(tx.getAmount()).isEqualTo(BigDecimal.valueOf(4500));
        verify(creatorService).updateBalance("creator-1", BigDecimal.valueOf(4500));
    }

    @Test
    @DisplayName("createFromEpisodeOrder - Combo order with multi-series unlocked contents and negative platform share handling")
    void createFromEpisodeOrder_ComboOrder() {
        Order order = Order.builder()
                .orderId("order-200")
                .itemType("COMBO")
                .itemId("combo-1")
                .totalAmount(BigDecimal.valueOf(20000))
                .vatAmount(BigDecimal.ZERO)
                .build();

        Series s1 = new Series();
        s1.setTitle("Series A");
        Season se1 = new Season();
        se1.setSeries(s1);
        Episode ep1 = new Episode();
        ep1.setEpisodeNumber(1);
        ep1.setSeason(se1);

        Series s2 = new Series();
        s2.setTitle("Series B");
        Season se2 = new Season();
        se2.setSeries(s2);
        Episode ep2 = new Episode();
        ep2.setEpisodeNumber(2);
        ep2.setSeason(se2);

        List<EpisodeUnlockedContent> contents = List.of(
                EpisodeUnlockedContent.builder().episode(ep1).build(),
                EpisodeUnlockedContent.builder().episode(ep2).build()
        );

        // High directPurchaseShareRatio (0.8) > baseUnlockShare (0.7) -> platformShareRatio = -0.1 < 0
        sampleTierDto.setDirectPurchaseShareRatio(0.8);

        when(comboEpisodeRepository.findCreatorIdByComboId("combo-1")).thenReturn(Optional.of("creator-1"));
        when(creatorService.getById("creator-1")).thenReturn(sampleCreatorDto);
        when(creatorConfigService.getConfigEntity()).thenReturn(sampleConfig);
        when(creatorService.getEntityById("creator-1")).thenReturn(sampleCreator);
        when(revenueTransactionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RevenueTransaction tx = service.createFromEpisodeOrder(order, contents);

        assertThat(tx).isNotNull();
        verify(creatorService).updateBalance(eq("creator-1"), any());
    }

    // =========================================================================
    // 2. getAllTransactions
    // =========================================================================

    @Test
    @DisplayName("getAllTransactions - Creator specific vs Global pagination")
    void getAllTransactions() {
        RevenueTransaction tx = RevenueTransaction.builder().revenueTransactionId("tx-1").build();
        Page<RevenueTransaction> page = new PageImpl<>(List.of(tx));

        // Creator specific
        when(revenueTransactionRepository.findByCreator_CreatorIdOrderByCreatedAtDesc(eq("creator-1"), any(Pageable.class))).thenReturn(page);
        when(revenueTransactionMapper.toDto(tx)).thenReturn(RevenueTransactionDto.builder().revenueTransactionId("tx-1").build());

        BasePageResponse<RevenueTransactionDto> res1 = service.getAllTransactions("creator-1", 0, 0); // tests page/size bounds
        assertThat(res1.getContent()).hasSize(1);

        // Global
        when(revenueTransactionRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class))).thenReturn(page);
        BasePageResponse<RevenueTransactionDto> res2 = service.getAllTransactions(null, 1, 10);
        assertThat(res2.getContent()).hasSize(1);
    }

    // =========================================================================
    // 3. getRevenueSummary & getRevenueTimeSeries
    // =========================================================================

    @Test
    @DisplayName("getRevenueSummary - Sums transaction types correctly")
    void getRevenueSummary() {
        LocalDateTime now = LocalDateTime.now();
        RevenueTransaction tx1 = RevenueTransaction.builder().amount(BigDecimal.valueOf(1000)).revenueTransactionType(RevenueTransactionType.CONTENT_SHARE).build();
        RevenueTransaction tx2 = RevenueTransaction.builder().amount(BigDecimal.valueOf(2000)).revenueTransactionType(RevenueTransactionType.PREMIUM_SHARE).build();
        RevenueTransaction tx3 = RevenueTransaction.builder().amount(BigDecimal.valueOf(500)).revenueTransactionType(RevenueTransactionType.PENALTY_DEDUCTION).build();

        when(revenueTransactionRepository.findByCreator_CreatorIdAndCreatedAtBetweenOrderByCreatedAtAsc("creator-1", now.minusDays(1), now))
                .thenReturn(List.of(tx1, tx2, tx3));

        RevenueSummaryResponseDto summary = service.getRevenueSummary("creator-1", now.minusDays(1), now);

        assertThat(summary.getTotalRevenueAmount()).isEqualTo(BigDecimal.valueOf(3000));
        assertThat(summary.getTotalPenaltyAmount()).isEqualTo(BigDecimal.valueOf(500));
    }

    @Test
    @DisplayName("getRevenueTimeSeries - Grouping units: HOUR, DAY, MONTH, YEAR")
    void getRevenueTimeSeries() {
        LocalDateTime now = LocalDateTime.now();
        RevenueTransaction tx = RevenueTransaction.builder().createdAt(now).amount(BigDecimal.valueOf(1000)).revenueTransactionType(RevenueTransactionType.CONTENT_SHARE).build();

        // < 7 days -> HOUR
        when(revenueTransactionRepository.findByCreatedAtBetweenOrderByCreatedAtAsc(any(), any())).thenReturn(List.of(tx));
        List<RevenueTimeSeriesResponseDto> resHour = service.getRevenueTimeSeries(null, now.minusDays(2), now);
        assertThat(resHour.get(0).getGroupUnit()).isEqualTo("HOUR");

        // < 30 days -> DAY
        List<RevenueTimeSeriesResponseDto> resDay = service.getRevenueTimeSeries(null, now.minusDays(15), now);
        assertThat(resDay.get(0).getGroupUnit()).isEqualTo("DAY");

        // < 365 days -> MONTH
        List<RevenueTimeSeriesResponseDto> resMonth = service.getRevenueTimeSeries(null, now.minusDays(100), now);
        assertThat(resMonth.get(0).getGroupUnit()).isEqualTo("MONTH");

        // >= 365 days -> YEAR
        List<RevenueTimeSeriesResponseDto> resYear = service.getRevenueTimeSeries(null, now.minusDays(400), now);
        assertThat(resYear.get(0).getGroupUnit()).isEqualTo("YEAR");
    }

    // =========================================================================
    // 4. getTotalUnsettledRevenueByEpisodeId & getTotalUnsettledRevenueBySeriesId
    // =========================================================================

    @Test
    @DisplayName("getTotalUnsettledRevenueByEpisodeId - Episode not found vs Success")
    void getTotalUnsettledRevenueByEpisodeId() {
        when(episodeRepository.existsById("ep-invalid")).thenReturn(false);
        assertThatThrownBy(() -> service.getTotalUnsettledRevenueByEpisodeId("ep-invalid"))
                .isInstanceOf(IllegalArgumentException.class);

        when(episodeRepository.existsById("ep-1")).thenReturn(true);
        when(revenueTransactionRepository.calculateUnsettledRevenueByEpisodeId("ep-1")).thenReturn(BigDecimal.valueOf(10000));
        when(subscriptionRevenueLogRepository.calculateUnsettledSubscriptionRevenueByEpisodeId("ep-1")).thenReturn(BigDecimal.valueOf(5000));

        TotalEpisodeRevenueDto dto = service.getTotalUnsettledRevenueByEpisodeId("ep-1");

        assertThat(dto.getUnsettledDirectAmount()).isEqualTo(BigDecimal.valueOf(10000));
        assertThat(dto.getUnsettledSubscriptionAmount()).isEqualTo(BigDecimal.valueOf(5000));
        assertThat(dto.getTotalUnsettledAmount()).isEqualTo(BigDecimal.valueOf(15000));
    }

    @Test
    @DisplayName("getTotalUnsettledRevenueBySeriesId - Series not found vs Success")
    void getTotalUnsettledRevenueBySeriesId() {
        when(seriesRepository.existsById("series-invalid")).thenReturn(false);
        assertThatThrownBy(() -> service.getTotalUnsettledRevenueBySeriesId("series-invalid"))
                .isInstanceOf(IllegalArgumentException.class);

        when(seriesRepository.existsById("series-1")).thenReturn(true);
        when(episodeRepository.findEpisodeIdsBySeriesId("series-1")).thenReturn(List.of("ep-1", "ep-2"));

        when(episodeRepository.existsById("ep-1")).thenReturn(true);
        when(revenueTransactionRepository.calculateUnsettledRevenueByEpisodeId("ep-1")).thenReturn(BigDecimal.valueOf(10000));
        when(subscriptionRevenueLogRepository.calculateUnsettledSubscriptionRevenueByEpisodeId("ep-1")).thenReturn(BigDecimal.valueOf(5000));

        when(episodeRepository.existsById("ep-2")).thenReturn(true);
        when(revenueTransactionRepository.calculateUnsettledRevenueByEpisodeId("ep-2")).thenReturn(BigDecimal.valueOf(20000));
        when(subscriptionRevenueLogRepository.calculateUnsettledSubscriptionRevenueByEpisodeId("ep-2")).thenReturn(BigDecimal.valueOf(10000));

        TotalSeriesRevenueDto dto = service.getTotalUnsettledRevenueBySeriesId("series-1");

        assertThat(dto.getUnsettledDirectAmount()).isEqualTo(BigDecimal.valueOf(30000));
        assertThat(dto.getUnsettledSubscriptionAmount()).isEqualTo(BigDecimal.valueOf(15000));
        assertThat(dto.getTotalUnsettledAmount()).isEqualTo(BigDecimal.valueOf(45000));
    }
}
