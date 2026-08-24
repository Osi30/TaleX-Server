package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.request.RatingRequest;
import com.talex.server.dtos.interaction.response.AccountRatingResponse;
import com.talex.server.dtos.interaction.response.SeriesRatingResponse;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.interaction.AccountRating;
import com.talex.server.entities.series.Series;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.interaction.AccountRatingRepository;
import com.talex.server.repositories.series.SeriesRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountRatingServiceImpl Tests")
class AccountRatingServiceImplTest {

    @Mock
    private AccountRatingRepository ratingRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SeriesRepository seriesRepository;

    @InjectMocks
    private AccountRatingServiceImpl service;

    private UUID accountId;
    private String seriesId;
    private Account account;
    private Series series;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        seriesId = "series-10";

        account = new Account();
        account.setAccountId(accountId);

        series = new Series();
        series.setSeriesId(seriesId);
    }

    @Test
    @DisplayName("upsertRating - Existing Rating update (delta != 0 and delta == 0)")
    void upsertRating_Existing() {
        AccountRating existingRating = AccountRating.builder()
                .rate(3.0)
                .build();

        when(ratingRepository.findByAccountAccountIdAndSeriesSeriesId(accountId, seriesId))
                .thenReturn(Optional.of(existingRating));

        // Delta != 0
        RatingRequest updateReq = new RatingRequest();
        updateReq.setAccountId(accountId);
        updateReq.setSeriesId(seriesId);
        updateReq.setRate(5.0);

        service.upsertRating(updateReq);

        assertThat(existingRating.getRate()).isEqualTo(5.0);
        verify(ratingRepository).save(existingRating);
        verify(ratingRepository).updateSeriesRatingMetrics(seriesId, 2.0, 0);

        // Delta == 0
        RatingRequest sameReq = new RatingRequest();
        sameReq.setAccountId(accountId);
        sameReq.setSeriesId(seriesId);
        sameReq.setRate(5.0);

        service.upsertRating(sameReq);
        verify(ratingRepository, times(1)).updateSeriesRatingMetrics(eq(seriesId), anyDouble(), eq(0L));
    }

    @Test
    @DisplayName("upsertRating - New Rating creation & validation exceptions")
    void upsertRating_New() {
        RatingRequest newReq = new RatingRequest();
        newReq.setAccountId(accountId);
        newReq.setSeriesId(seriesId);
        newReq.setRate(4.0);

        when(ratingRepository.findByAccountAccountIdAndSeriesSeriesId(accountId, seriesId))
                .thenReturn(Optional.empty());

        // Account missing
        when(accountRepository.findById(accountId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upsertRating(newReq))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Tài khoản không tồn tại");

        // Series missing
        when(accountRepository.findById(accountId)).thenReturn(Optional.of(account));
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.upsertRating(newReq))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Series không tồn tại");

        // Success creation
        when(seriesRepository.findById(seriesId)).thenReturn(Optional.of(series));
        service.upsertRating(newReq);

        verify(ratingRepository).save(any(AccountRating.class));
        verify(ratingRepository).updateSeriesRatingMetrics(seriesId, 4.0, 1);
        verify(accountRepository).updateLastInteractionTime(any(), eq(accountId));
    }

    @Test
    @DisplayName("deleteRating - Missing vs Existing rating")
    void deleteRating() {
        // Missing
        when(ratingRepository.findByAccountAccountIdAndSeriesSeriesId(accountId, seriesId))
                .thenReturn(Optional.empty());
        assertThatThrownBy(() -> service.deleteRating(accountId, seriesId))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Bạn chưa đánh giá series này");

        // Existing
        AccountRating rating = AccountRating.builder().rate(4.5).build();
        when(ratingRepository.findByAccountAccountIdAndSeriesSeriesId(accountId, seriesId))
                .thenReturn(Optional.of(rating));

        service.deleteRating(accountId, seriesId);

        verify(ratingRepository).delete(rating);
        verify(ratingRepository).updateSeriesRatingMetrics(seriesId, -4.5, -1);
    }

    @Test
    @DisplayName("getRatingsByAccount & getRatingsBySeries - Validation & Slice retrieval")
    void getRatingsByAccountAndSeries() {
        Pageable pageable = PageRequest.of(0, 10);

        // Missing account
        when(accountRepository.existsById(accountId)).thenReturn(false);
        assertThatThrownBy(() -> service.getRatingsByAccount(accountId, pageable))
                .isInstanceOf(RuntimeException.class);

        // Valid account
        when(accountRepository.existsById(accountId)).thenReturn(true);
        Slice<AccountRatingResponse> accountSlice = new SliceImpl<>(List.of(), pageable, false);
        when(ratingRepository.findRatingsByAccountId(accountId, pageable)).thenReturn(accountSlice);
        assertThat(service.getRatingsByAccount(accountId, pageable)).isNotNull();

        // Missing series
        when(seriesRepository.existsById(seriesId)).thenReturn(false);
        assertThatThrownBy(() -> service.getRatingsBySeries(seriesId, pageable))
                .isInstanceOf(RuntimeException.class);

        // Valid series
        when(seriesRepository.existsById(seriesId)).thenReturn(true);
        Slice<SeriesRatingResponse> seriesSlice = new SliceImpl<>(List.of(), pageable, false);
        when(ratingRepository.findRatingsBySeriesId(seriesId, pageable)).thenReturn(seriesSlice);
        assertThat(service.getRatingsBySeries(seriesId, pageable)).isNotNull();
    }
}
