package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.response.AccountLikeResponse;
import com.talex.server.dtos.interaction.response.EpisodeLikeResponse;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.interaction.AccountLike;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.AccountLikeRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Slice;
import org.springframework.data.domain.SliceImpl;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountLikeServiceImpl Tests")
class AccountLikeServiceImplTest {

    @Mock
    private AccountLikeRepository accountLikeRepository;

    @InjectMocks
    private AccountLikeServiceImpl service;

    private UUID accountId;
    private String episodeId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        episodeId = "ep-1";
    }

    @Test
    @DisplayName("likeEpisode - Success, Already Exists, and DataIntegrityViolation branches")
    void likeEpisode() {
        // Success
        when(accountLikeRepository.insertLikeDirectly(eq(accountId), eq(episodeId), any())).thenReturn(1);
        service.likeEpisode(accountId, episodeId);
        verify(accountLikeRepository).insertLikeDirectly(eq(accountId), eq(episodeId), any());

        // Already exists (rowsAffected == 0)
        when(accountLikeRepository.insertLikeDirectly(eq(accountId), eq(episodeId), any())).thenReturn(0);
        assertThatThrownBy(() -> service.likeEpisode(accountId, episodeId))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.LIKE_ALREADY_EXISTS);

        // DataIntegrityViolationException
        when(accountLikeRepository.insertLikeDirectly(eq(accountId), eq(episodeId), any()))
                .thenThrow(new DataIntegrityViolationException("FK failure"));
        assertThatThrownBy(() -> service.likeEpisode(accountId, episodeId))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.LIKE_NOT_FOUND);
    }

    @Test
    @DisplayName("unlikeEpisode - Success vs Not Found")
    void unlikeEpisode() {
        // Success
        when(accountLikeRepository.deleteByAccountIdAndEpisodeId(accountId, episodeId)).thenReturn(1);
        service.unlikeEpisode(accountId, episodeId);

        // Not Found (0 rows deleted)
        when(accountLikeRepository.deleteByAccountIdAndEpisodeId(accountId, episodeId)).thenReturn(0);
        assertThatThrownBy(() -> service.unlikeEpisode(accountId, episodeId))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.LIKE_NOT_FOUND);
    }

    @Test
    @DisplayName("getLikesByEpisode - Returns mapped Slice")
    void getLikesByEpisode() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = new Account();
        account.setAccountId(accountId);
        account.setUsername("user1");
        account.setAvatarUrl("avatar.jpg");

        AccountLike like = new AccountLike();
        like.setAccount(account);
        like.setCreatedAt(LocalDateTime.now());

        Slice<AccountLike> slice = new SliceImpl<>(List.of(like), pageable, false);
        when(accountLikeRepository.findByEpisodeEpisodeId(episodeId, pageable)).thenReturn(slice);

        Slice<EpisodeLikeResponse> res = service.getLikesByEpisode(episodeId, pageable);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(res.getContent().get(0).getUsername()).isEqualTo("user1");
    }

    @Test
    @DisplayName("getLikesByAccount - Maps response with season/series present vs null fallbacks")
    void getLikesByAccount() {
        Pageable pageable = PageRequest.of(0, 10);

        // Episode 1 with full Series info
        Series series = new Series();
        series.setTitle("My Series");
        series.setCoverUrl("cover.jpg");

        Season season = new Season();
        season.setSeries(series);

        Episode ep1 = new Episode();
        ep1.setEpisodeId("ep-1");
        ep1.setTitle("Ep 1 Title");
        ep1.setEpisodeNumber(1);
        ep1.setSeason(season);

        AccountLike l1 = new AccountLike();
        l1.setEpisode(ep1);
        l1.setCreatedAt(LocalDateTime.now());

        // Episode 2 with null Season/Series info
        Episode ep2 = new Episode();
        ep2.setEpisodeId("ep-2");
        ep2.setTitle("Ep 2 Title");
        ep2.setEpisodeNumber(2);
        ep2.setSeason(null);

        AccountLike l2 = new AccountLike();
        l2.setEpisode(ep2);
        l2.setCreatedAt(LocalDateTime.now());

        Slice<AccountLike> slice = new SliceImpl<>(List.of(l1, l2), pageable, false);
        when(accountLikeRepository.findByAccountAccountId(accountId, pageable)).thenReturn(slice);

        Slice<AccountLikeResponse> res = service.getLikesByAccount(accountId, pageable);

        assertThat(res.getContent()).hasSize(2);
        assertThat(res.getContent().get(0).getSeriesTitle()).isEqualTo("My Series");
        assertThat(res.getContent().get(0).getSeriesCoverUrl()).isEqualTo("cover.jpg");

        assertThat(res.getContent().get(1).getSeriesTitle()).isEqualTo("N/A");
        assertThat(res.getContent().get(1).getSeriesCoverUrl()).isNull();
    }
}
