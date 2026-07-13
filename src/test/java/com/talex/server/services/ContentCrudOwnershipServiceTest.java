package com.talex.server.services;

import com.talex.server.dtos.requests.EpisodeRequestDto;
import com.talex.server.dtos.requests.SeasonRequestDto;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.creator.Creator;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.AccountRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.repositories.series.SeasonRepository;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.services.impls.EpisodeServiceImpl;
import com.talex.server.services.impls.SeasonServiceImpl;
import com.talex.server.services.audit.ContentAuditLogger;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ContentCrudOwnershipServiceTest {
    private static final UUID ACCOUNT_ID = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final String CURRENT_CREATOR_ID = "creator-current";
    private static final String OTHER_CREATOR_ID = "creator-other";

    @Mock
    private ICreatorService creatorService;
    @Mock
    private SeriesService seriesService;
    @Mock
    private SeasonService seasonService;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private ContentAuditLogger contentAuditLogger;

    private SeasonServiceImpl seasonServiceImpl;
    private EpisodeServiceImpl episodeServiceImpl;

    @BeforeEach
    void setUp() {
        authenticateCreator();
        Creator creator = new Creator();
        creator.setCreatorId(CURRENT_CREATOR_ID);
        when(creatorService.getEntityByAccountId(ACCOUNT_ID)).thenReturn(creator);

        ContentOwnershipService ownershipService = new ContentOwnershipService(creatorService);
        seasonServiceImpl = new SeasonServiceImpl(seasonRepository, seriesService, ownershipService, contentAuditLogger);
        episodeServiceImpl = new EpisodeServiceImpl(
                episodeRepository,
                mediaRepository,
                accountRepository,
                seasonService,
                ownershipService,
                contentAuditLogger);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void seasonRejectsGetCreateUpdateAndDeleteForAnotherCreator() {
        Series otherSeries = seriesOwnedBy(OTHER_CREATOR_ID);
        Season otherSeason = seasonOwnedBy(OTHER_CREATOR_ID, otherSeries);
        when(seriesService.findActiveEntity("series-other")).thenReturn(otherSeries);
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse("season-other"))
                .thenReturn(Optional.of(otherSeason));

        assertThrows(ContentModuleException.class,
                () -> seasonServiceImpl.getById("season-other", ACCOUNT_ID.toString()));
        assertThrows(ContentModuleException.class,
                () -> seasonServiceImpl.create(
                        "series-other", new SeasonRequestDto(), ACCOUNT_ID.toString()));
        assertThrows(ContentModuleException.class,
                () -> seasonServiceImpl.update(
                        "season-other", new SeasonRequestDto(), ACCOUNT_ID.toString()));
        assertThrows(ContentModuleException.class,
                () -> seasonServiceImpl.delete("season-other", ACCOUNT_ID.toString()));

        verify(seasonRepository, never()).save(org.mockito.ArgumentMatchers.any(Season.class));
    }

    @Test
    void episodeRejectsGetCreateUpdateAndDeleteForAnotherCreator() {
        Series otherSeries = seriesOwnedBy(OTHER_CREATOR_ID);
        Season otherSeason = seasonOwnedBy(OTHER_CREATOR_ID, otherSeries);
        Episode otherEpisode = episodeOwnedBy(OTHER_CREATOR_ID, otherSeason);
        when(seasonService.findActiveEntity("season-other")).thenReturn(otherSeason);
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse("episode-other"))
                .thenReturn(Optional.of(otherEpisode));

        assertThrows(ContentModuleException.class,
                () -> episodeServiceImpl.getById("episode-other", ACCOUNT_ID.toString()));
        assertThrows(ContentModuleException.class,
                () -> episodeServiceImpl.create(
                        "season-other", new EpisodeRequestDto(), ACCOUNT_ID.toString()));
        assertThrows(ContentModuleException.class,
                () -> episodeServiceImpl.update(
                        "episode-other", new EpisodeRequestDto(), ACCOUNT_ID.toString()));
        assertThrows(ContentModuleException.class,
                () -> episodeServiceImpl.delete("episode-other", ACCOUNT_ID.toString()));

        verify(episodeRepository, never()).save(org.mockito.ArgumentMatchers.any(Episode.class));
    }

    private void authenticateCreator() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(
                        ACCOUNT_ID.toString(),
                        null,
                        List.of(new SimpleGrantedAuthority("ROLE_CREATOR"))));
    }

    private Series seriesOwnedBy(String creatorId) {
        Series series = new Series();
        series.setSeriesId("series-other");
        series.setCreator(Creator.builder()
                .creatorId(creatorId)
                .build());
        return series;
    }

    private Season seasonOwnedBy(String creatorId, Series series) {
        Season season = new Season();
        season.setSeasonId("season-other");
        season.setCreatorId(creatorId);
        season.setSeries(series);
        return season;
    }

    private Episode episodeOwnedBy(String creatorId, Season season) {
        Episode episode = new Episode();
        episode.setEpisodeId("episode-other");
        episode.setCreatorId(creatorId);
        episode.setSeason(season);
        return episode;
    }
}
