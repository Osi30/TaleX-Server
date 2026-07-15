package com.talex.server.services;

import com.talex.server.dtos.requests.series.EpisodeUnlockSettingsRequestDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.auth.Role;
import com.talex.server.entities.media.Media;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.series.*;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.series.CategoryRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.TagRepository;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.series.impls.EpisodeServiceImpl;
import com.talex.server.services.series.SeasonService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
class EpisodeScheduledPublishServiceTest {
    private static final UUID ACTOR_UUID = UUID.fromString("00000000-0000-0000-0000-000000000001");
    private static final String ACTOR_ID = ACTOR_UUID.toString();

    @Mock
    private EpisodeRepository episodeRepository;
    @Mock
    private MediaRepository mediaRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private SeasonService seasonService;
    @Mock
    private ContentOwnershipService contentOwnershipService;
    @Mock
    private ContentAuditLogger contentAuditLogger;

    private EpisodeServiceImpl episodeService;

    @BeforeEach
    void setUp() {
        episodeService = new EpisodeServiceImpl(
                episodeRepository, mediaRepository, tagRepository, categoryRepository, accountRepository, seasonService, contentOwnershipService, contentAuditLogger);
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

    @Test
    void creatorRoleCanUpdateUnlockSettings() {
        Episode episode = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-paid");
        stubActiveEpisode(episode);
        stubAccountRole(2L);

        episodeService.updateUnlockSettings(
                episode.getEpisodeId(),
                new EpisodeUnlockSettingsRequestDto(EpisodeUnlockType.PAID, 10_000L),
                ACTOR_ID);

        assertEquals(EpisodeUnlockType.PAID, episode.getUnlockType());
        assertEquals(10_000L, episode.getPriceVnd());
    }

    @Test
    void nonCreatorRoleCannotUpdateUnlockSettings() {
        Episode episode = episodeWithParents(SeriesStatus.DRAFT, SeasonStatus.DRAFT, "episode-blocked");
        stubActiveEpisode(episode);
        stubAccountRole(1L);

        assertThrows(ContentModuleException.class, () -> episodeService.updateUnlockSettings(
                episode.getEpisodeId(),
                new EpisodeUnlockSettingsRequestDto(EpisodeUnlockType.PAID, 10_000L),
                ACTOR_ID));

        assertEquals(EpisodeUnlockType.FREE, episode.getUnlockType());
        assertEquals(0L, episode.getPriceVnd());
    }

    private void stubActiveEpisode(Episode episode) {
        lenient().when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episode.getEpisodeId()))
                .thenReturn(Optional.of(episode));
        lenient().when(episodeRepository.lockByEpisodeIdAndIsDeletedFalse(episode.getEpisodeId()))
                .thenReturn(Optional.of(episode));
    }

    private void stubAccountRole(long roleId) {
        Role role = Role.builder()
                .roleId(roleId)
                .code(roleId == 2L ? "CREATOR" : "VIEWER")
                .roleName(roleId == 2L ? "Creator" : "Viewer")
                .build();
        Account account = Account.builder()
                .accountId(ACTOR_UUID)
                .role(role)
                .build();

        lenient().when(accountRepository.findById(ACTOR_UUID))
                .thenReturn(Optional.of(account));
    }

    private void stubReadyMedia(Episode episode, List<Media> media) {
        long count = media.size();
        lenient().when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(
                eq(episode.getEpisodeId())))
                .thenReturn(count);
        lenient().when(mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                eq(episode.getEpisodeId()), eq(ContentApprovalStatus.APPROVED)))
                .thenReturn(false);
        lenient().when(mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                eq(episode.getEpisodeId()), any(), any()))
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
