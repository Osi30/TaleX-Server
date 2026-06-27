package com.talex.server.services;

import com.talex.server.entities.Episode;
import com.talex.server.entities.Media;
import com.talex.server.entities.Season;
import com.talex.server.entities.Series;
import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
import com.talex.server.enums.MediaStatus;
import com.talex.server.enums.SeasonStatus;
import com.talex.server.enums.SeriesStatus;
import com.talex.server.repositories.EpisodeRepository;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.services.impls.EpisodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EpisodeScheduledPublishServiceTest {
    private static final String ACTOR_ID = "creator-1";

    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private SeasonService seasonService;
    @Mock
    private ContentOwnershipService contentOwnershipService;

    private EpisodeServiceImpl episodeService;

    @BeforeEach
    void setUp() {
        episodeService = new EpisodeServiceImpl(
                episodeRepository, mediaRepository, seasonService, contentOwnershipService);
        lenient().when(contentOwnershipService.isPrivileged()).thenReturn(true);
        when(episodeRepository.save(any(Episode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void schedulingFirstEpisodeOfNewSeasonHidesSeasonButKeepsPublishedSeriesVisible() {
        Episode episode = episodeWithParents(SeriesStatus.PUBLISHED, SeasonStatus.PUBLISHED);
        Media media = approvedMedia(MediaStatus.ACTIVE);
        stubEpisodeAndMedia(episode, media, 0, 3);

        episodeService.schedulePublish(
                episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);

        assertEquals(SeasonStatus.HIDDEN, episode.getSeason().getStatus());
        assertEquals(SeriesStatus.PUBLISHED, episode.getSeason().getSeries().getStatus());
        assertEquals(EpisodeStatus.HIDDEN, episode.getStatus());
        assertEquals(MediaStatus.HIDDEN, media.getStatus());
    }

    @Test
    void publishingFirstEpisodeOfSeriesPublishesBothSeasonAndSeries() {
        Episode episode = episodeWithParents(SeriesStatus.HIDDEN, SeasonStatus.HIDDEN);
        episode.setStatus(EpisodeStatus.HIDDEN);
        episode.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));
        Media media = approvedMedia(MediaStatus.HIDDEN);
        stubEpisodeAndMedia(episode, media, 0, 0);

        episodeService.publishScheduled(episode.getEpisodeId(), "system_cron");

        assertEquals(SeasonStatus.PUBLISHED, episode.getSeason().getStatus());
        assertEquals(SeriesStatus.PUBLISHED, episode.getSeason().getSeries().getStatus());
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
        assertEquals(MediaStatus.ACTIVE, media.getStatus());
    }

    private void stubEpisodeAndMedia(
            Episode episode,
            Media media,
            long publishedInSeason,
            long publishedInSeries) {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episode.getEpisodeId()))
                .thenReturn(Optional.of(episode));
        when(mediaRepository.findAllByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
                any(), any(), any(), any())).thenReturn(List.of(media));
        when(episodeRepository.countBySeasonIdExcludingEpisodeAndStatus(
                episode.getSeason().getSeasonId(), episode.getEpisodeId(), EpisodeStatus.PUBLISHED))
                .thenReturn(publishedInSeason);
        when(episodeRepository.countBySeriesIdExcludingEpisodeAndStatus(
                episode.getSeason().getSeries().getSeriesId(), episode.getEpisodeId(), EpisodeStatus.PUBLISHED))
                .thenReturn(publishedInSeries);
    }

    private Episode episodeWithParents(SeriesStatus seriesStatus, SeasonStatus seasonStatus) {
        Series series = new Series();
        series.setSeriesId("series-1");
        series.setStatus(seriesStatus);
        series.setContentType(ContentType.COMIC);

        Season season = new Season();
        season.setSeasonId("season-2");
        season.setSeries(series);
        season.setStatus(seasonStatus);

        Episode episode = new Episode();
        episode.setEpisodeId("episode-1");
        episode.setSeason(season);
        episode.setContentType(ContentType.COMIC);
        episode.setStatus(EpisodeStatus.DRAFT);
        return episode;
    }

    private Media approvedMedia(MediaStatus status) {
        Media media = new Media();
        media.setStatus(status);
        media.setApprovalStatus(ContentApprovalStatus.APPROVED);
        return media;
    }
}
