package com.talex.server.services;

import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.series.ContentApprovalStatus;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.EpisodeStatus;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.series.SeasonStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.MediaRepository;
import com.talex.server.repositories.series.EpisodeRepository;
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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
        lenient().when(episodeRepository.save(any(Episode.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void newHierarchyStaysScheduledUntilDueThenPublishesTogether() {
        Episode episode = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-1");
        Media media = approvedMedia(MediaStatus.ACTIVE);
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(media));

        episodeService.schedulePublish(episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);

        assertEquals(SeriesStatus.SCHEDULED, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.SCHEDULED, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getStatus());
        assertEquals(MediaStatus.ACTIVE, media.getStatus());

        episode.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));
        episodeService.publishScheduled(episode.getEpisodeId(), "system_cron");

        assertEquals(SeriesStatus.PUBLISHED, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.PUBLISHED, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
        assertEquals(MediaStatus.ACTIVE, media.getStatus());
        assertNull(episode.getScheduledPublishAt());
    }

    @Test
    void addingEpisodeToPublishedHierarchySchedulesOnlyThatEpisode() {
        Episode episode = episodeWithParents(SeriesStatus.PUBLISHED, SeasonStatus.PUBLISHED, "episode-2");
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.schedulePublish(episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);

        assertEquals(SeriesStatus.PUBLISHED, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.PUBLISHED, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getStatus());
    }

    @Test
    void newSeasonIsScheduledWithoutChangingPublishedSeriesOrOtherHiddenSeason() {
        Episode episode = episodeWithParents(SeriesStatus.PUBLISHED, SeasonStatus.DRAFT, "episode-new-season");
        Season hiddenSeason = new Season();
        hiddenSeason.setStatus(SeasonStatus.HIDDEN);
        episode.getSeason().getSeries().getSeasons().add(hiddenSeason);
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.schedulePublish(episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);

        assertEquals(SeriesStatus.PUBLISHED, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.SCHEDULED, episode.getSeason().getStatus());
        assertEquals(SeasonStatus.HIDDEN, hiddenSeason.getStatus());
    }

    @Test
    void manuallyHiddenParentsAreNeverAutomaticallyUnhidden() {
        Episode episode = episodeWithParents(SeriesStatus.HIDDEN, SeasonStatus.HIDDEN, "episode-1");
        episode.setStatus(EpisodeStatus.SCHEDULED);
        episode.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.publishScheduled(episode.getEpisodeId(), "system_cron");

        assertEquals(SeriesStatus.HIDDEN, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.HIDDEN, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
    }

    @Test
    void hiddenSeasonPreventsEmptyDraftSeriesFromBeingAutoPublished() {
        Episode episode = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.HIDDEN, "episode-1");
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.schedulePublish(episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);
        assertEquals(SeriesStatus.DRAFT, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.HIDDEN, episode.getSeason().getStatus());

        episode.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));
        episodeService.publishScheduled(episode.getEpisodeId(), "system_cron");

        assertEquals(SeriesStatus.DRAFT, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.HIDDEN, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
    }

    @Test
    void earliestOfMultipleScheduledEpisodesCanPublishTheirSharedNewParents() {
        Series series = series(SeriesStatus.DRAFT);
        Season season = season(series, SeasonStatus.DRAFT);
        Episode laterEpisode = episode(season, "episode-later");
        Episode earlierEpisode = episode(season, "episode-earlier");
        stubActiveEpisode(laterEpisode);
        stubActiveEpisode(earlierEpisode);
        stubReadyMedia(laterEpisode, List.of(approvedMedia(MediaStatus.ACTIVE)));
        stubReadyMedia(earlierEpisode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.schedulePublish(laterEpisode.getEpisodeId(), LocalDateTime.now().plusDays(2), ACTOR_ID);
        episodeService.schedulePublish(earlierEpisode.getEpisodeId(), LocalDateTime.now().plusDays(1), ACTOR_ID);
        earlierEpisode.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));

        episodeService.publishScheduled(earlierEpisode.getEpisodeId(), "system_cron");

        assertEquals(SeriesStatus.PUBLISHED, series.getStatus());
        assertEquals(SeasonStatus.PUBLISHED, season.getStatus());
        assertEquals(EpisodeStatus.PUBLISHED, earlierEpisode.getStatus());
        assertEquals(EpisodeStatus.SCHEDULED, laterEpisode.getStatus());
    }

    @Test
    void hidingOnlyScheduledEpisodeCancelsScheduleAndRestoresNewParentsToDraft() {
        Episode episode = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-1");
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.schedulePublish(episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);
        episodeService.hide(episode.getEpisodeId(), ACTOR_ID);

        assertEquals(SeriesStatus.DRAFT, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.DRAFT, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.HIDDEN, episode.getStatus());
        assertNull(episode.getScheduledPublishAt());
    }

    @Test
    void cancellingScheduleRestoresNewParentsToDraft() {
        Episode episode = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-1");
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.schedulePublish(episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID);
        episodeService.cancelSchedule(episode.getEpisodeId(), ACTOR_ID);

        assertEquals(SeriesStatus.DRAFT, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.DRAFT, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.DRAFT, episode.getStatus());
        assertNull(episode.getScheduledPublishAt());
    }

    @Test
    void schedulingRequiresFutureTimeAndNonPublishedEpisode() {
        Episode draft = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-draft");
        stubActiveEpisode(draft);

        assertThrows(ContentModuleException.class, () -> episodeService.schedulePublish(
                draft.getEpisodeId(), LocalDateTime.now().minusMinutes(1), ACTOR_ID));

        Episode published = episodeWithParents(SeriesStatus.PUBLISHED, SeasonStatus.PUBLISHED, "episode-published");
        published.setStatus(EpisodeStatus.PUBLISHED);
        stubActiveEpisode(published);

        assertThrows(ContentModuleException.class, () -> episodeService.schedulePublish(
                published.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID));
    }

    @Test
    void cronRejectsEpisodeThatIsNotScheduledOrNotDue() {
        Episode draft = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-draft");
        stubActiveEpisode(draft);
        assertThrows(ContentModuleException.class,
                () -> episodeService.publishScheduled(draft.getEpisodeId(), "system_cron"));

        Episode future = episodeWithParents(SeriesStatus.SCHEDULED, SeasonStatus.SCHEDULED, "episode-future");
        future.setStatus(EpisodeStatus.SCHEDULED);
        future.setScheduledPublishAt(LocalDateTime.now().plusHours(1));
        stubActiveEpisode(future);
        assertThrows(ContentModuleException.class,
                () -> episodeService.publishScheduled(future.getEpisodeId(), "system_cron"));
    }

    @Test
    void cronDoesNotPublishAnythingWhenMediaIsNoLongerReady() {
        Episode episode = episodeWithParents(SeriesStatus.SCHEDULED, SeasonStatus.SCHEDULED, "episode-1");
        episode.setStatus(EpisodeStatus.SCHEDULED);
        episode.setScheduledPublishAt(LocalDateTime.now().minusMinutes(1));
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of());

        assertThrows(ContentModuleException.class,
                () -> episodeService.publishScheduled(episode.getEpisodeId(), "system_cron"));

        assertEquals(SeriesStatus.SCHEDULED, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.SCHEDULED, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.SCHEDULED, episode.getStatus());
    }

    @Test
    void deletedParentPreventsScheduling() {
        Episode episode = episodeWithParents(SeriesStatus.DELETED, SeasonStatus.DRAFT, "episode-1");
        stubActiveEpisode(episode);

        assertThrows(ContentModuleException.class, () -> episodeService.schedulePublish(
                episode.getEpisodeId(), LocalDateTime.now().plusHours(1), ACTOR_ID));
    }

    @Test
    void publishingEpisodeForcesHiddenParentsToPublished() {
        Episode episode = episodeWithParents(SeriesStatus.HIDDEN, SeasonStatus.HIDDEN, "episode-hidden-parents");
        stubActiveEpisode(episode);
        stubReadyMedia(episode, List.of(approvedMedia(MediaStatus.ACTIVE)));

        episodeService.publish(episode.getEpisodeId(), ACTOR_ID);

        assertEquals(SeriesStatus.PUBLISHED, episode.getSeason().getSeries().getStatus());
        assertEquals(SeasonStatus.PUBLISHED, episode.getSeason().getStatus());
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
    }

    private void stubActiveEpisode(Episode episode) {
        lenient().when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episode.getEpisodeId()))
                .thenReturn(Optional.of(episode));
        lenient().when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(episode.getEpisodeId()))
                .thenReturn(Optional.of(episode));
    }

    private void stubReadyMedia(Episode episode, List<Media> media) {
        long count = media.size();
        lenient().when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(
                eq(episode.getEpisodeId())))
                .thenReturn(count);
        lenient().when(mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndApprovalStatusAndIsDeletedFalse(
                eq(episode.getEpisodeId()), any(), any(), eq(ContentApprovalStatus.APPROVED)))
                .thenReturn(count);
    }

    private Episode episodeWithParents(
            SeriesStatus seriesStatus,
            SeasonStatus seasonStatus,
            String episodeId) {
        return episode(season(series(seriesStatus), seasonStatus), episodeId);
    }

    private Series series(SeriesStatus status) {
        Series series = new Series();
        series.setSeriesId("series-1");
        series.setStatus(status);
        series.setContentType(ContentType.COMIC);
        return series;
    }

    private Season season(Series series, SeasonStatus status) {
        Season season = new Season();
        season.setSeasonId("season-1");
        season.setSeries(series);
        season.setStatus(status);
        return season;
    }

    private Episode episode(Season season, String episodeId) {
        Episode episode = new Episode();
        episode.setEpisodeId(episodeId);
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