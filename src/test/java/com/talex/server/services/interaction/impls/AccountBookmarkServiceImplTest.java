package com.talex.server.services.interaction.impls;

import com.talex.server.dtos.interaction.response.AccountBookmarkResponse;
import com.talex.server.dtos.interaction.response.EpisodeBookmarkResponse;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.interaction.AccountBookmark;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.exceptions.codes.InteractionErrorCode;
import com.talex.server.exceptions.details.InteractionException;
import com.talex.server.repositories.interaction.AccountBookmarkRepository;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AccountBookmarkServiceImpl Tests")
class AccountBookmarkServiceImplTest {

    @Mock
    private AccountBookmarkRepository accountBookmarkRepository;

    @InjectMocks
    private AccountBookmarkServiceImpl service;

    private UUID accountId;
    private String episodeId;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        episodeId = "ep-1";
    }

    @Test
    @DisplayName("bookmarkEpisode - Success, Already Exists, and DataIntegrityViolation branches")
    void bookmarkEpisode() {
        // Success
        when(accountBookmarkRepository.insertBookmarkDirectly(accountId, episodeId)).thenReturn(1);
        service.bookmarkEpisode(accountId, episodeId);
        verify(accountBookmarkRepository).insertBookmarkDirectly(accountId, episodeId);

        // Already exists (rowsAffected == 0)
        when(accountBookmarkRepository.insertBookmarkDirectly(accountId, episodeId)).thenReturn(0);
        assertThatThrownBy(() -> service.bookmarkEpisode(accountId, episodeId))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.BOOKMARK_ALREADY_EXISTS);

        // DataIntegrityViolationException
        when(accountBookmarkRepository.insertBookmarkDirectly(accountId, episodeId))
                .thenThrow(new DataIntegrityViolationException("FK failure"));
        assertThatThrownBy(() -> service.bookmarkEpisode(accountId, episodeId))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.BOOKMARK_NOT_FOUND);
    }

    @Test
    @DisplayName("unbookmarkEpisode - Success vs Not Found")
    void unbookmarkEpisode() {
        // Success
        when(accountBookmarkRepository.deleteByAccountIdAndEpisodeId(accountId, episodeId)).thenReturn(1);
        service.unbookmarkEpisode(accountId, episodeId);

        // Not Found (0 rows affected)
        when(accountBookmarkRepository.deleteByAccountIdAndEpisodeId(accountId, episodeId)).thenReturn(0);
        assertThatThrownBy(() -> service.unbookmarkEpisode(accountId, episodeId))
                .isInstanceOf(InteractionException.class)
                .extracting("errorCode")
                .isEqualTo(InteractionErrorCode.BOOKMARK_NOT_FOUND);
    }

    @Test
    @DisplayName("getBookmarksByEpisode - Returns mapped Slice")
    void getBookmarksByEpisode() {
        Pageable pageable = PageRequest.of(0, 10);

        Account account = new Account();
        account.setAccountId(accountId);
        account.setUsername("user1");
        account.setAvatarUrl("avatar.jpg");

        AccountBookmark bookmark = new AccountBookmark();
        bookmark.setAccount(account);
        bookmark.setCreatedAt(LocalDateTime.now());

        Slice<AccountBookmark> slice = new SliceImpl<>(List.of(bookmark), pageable, false);
        when(accountBookmarkRepository.findByEpisodeEpisodeId(episodeId, pageable)).thenReturn(slice);

        Slice<EpisodeBookmarkResponse> res = service.getBookmarksByEpisode(episodeId, pageable);

        assertThat(res.getContent()).hasSize(1);
        assertThat(res.getContent().get(0).getAccountId()).isEqualTo(accountId);
        assertThat(res.getContent().get(0).getUsername()).isEqualTo("user1");
    }

    @Test
    @DisplayName("getBookmarksByAccount - Maps response with season/series present vs null fallbacks")
    void getBookmarksByAccount() {
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

        AccountBookmark b1 = new AccountBookmark();
        b1.setEpisode(ep1);
        b1.setCreatedAt(LocalDateTime.now());

        // Episode 2 with null Season/Series info
        Episode ep2 = new Episode();
        ep2.setEpisodeId("ep-2");
        ep2.setTitle("Ep 2 Title");
        ep2.setEpisodeNumber(2);
        ep2.setSeason(null);

        AccountBookmark b2 = new AccountBookmark();
        b2.setEpisode(ep2);
        b2.setCreatedAt(LocalDateTime.now());

        Slice<AccountBookmark> slice = new SliceImpl<>(List.of(b1, b2), pageable, false);
        when(accountBookmarkRepository.findByAccountAccountId(accountId, pageable)).thenReturn(slice);

        Slice<AccountBookmarkResponse> res = service.getBookmarksByAccount(accountId, pageable);

        assertThat(res.getContent()).hasSize(2);
        assertThat(res.getContent().get(0).getSeriesTitle()).isEqualTo("My Series");
        assertThat(res.getContent().get(0).getSeriesCoverUrl()).isEqualTo("cover.jpg");

        assertThat(res.getContent().get(1).getSeriesTitle()).isEqualTo("N/A");
        assertThat(res.getContent().get(1).getSeriesCoverUrl()).isNull();
    }
}
