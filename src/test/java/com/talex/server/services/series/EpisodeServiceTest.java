package com.talex.server.services.series;

import com.talex.server.dtos.requests.series.EpisodeRequestDto;
import com.talex.server.dtos.requests.series.EpisodeUnlockSettingsRequestDto;
import com.talex.server.dtos.responses.series.EpisodeResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.auth.Role;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.media.MediaStatus;
import com.talex.server.enums.media.MediaType;
import com.talex.server.enums.series.*;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.NotificationRepository;
import com.talex.server.repositories.auth.AccountRepository;
import com.talex.server.repositories.media.MediaRepository;
import com.talex.server.repositories.series.CategoryRepository;
import com.talex.server.repositories.series.EpisodeLogRepository;
import com.talex.server.repositories.series.EpisodeRepository;
import com.talex.server.repositories.series.TagRepository;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.series.impls.ContentCascadeDeleteHelper;
import com.talex.server.services.series.impls.EpisodeServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("EpisodeService Unit Tests - Covering Ep_001, Ep_002, Ep_003")
class EpisodeServiceTest {

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
    private EpisodeLogRepository episodeLogRepository;

    @Mock
    private SeasonService seasonService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private ContentOwnershipService contentOwnershipService;

    @Mock
    private ContentAuditLogger contentAuditLogger;

    @Mock
    private ContentCascadeDeleteHelper contentCascadeDeleteHelper;

    @InjectMocks
    private EpisodeServiceImpl episodeService;

    private Series series;
    private Season season;
    private Episode episode;
    private Creator creator;
    private Account account;
    private EpisodeRequestDto episodeRequestDto;
    private final String accountId = "00000000-0000-0000-0000-000000000001";
    private final String seriesId = "series-1";
    private final String seasonId = "season-1";
    private final String episodeId = "episode-1";

    @BeforeEach
    void setUp() {
        account = new Account();
        account.setAccountId(UUID.fromString(accountId));
        Role creatorRole = new Role();
        creatorRole.setRoleId(2L);
        account.setRole(creatorRole);

        creator = new Creator();
        creator.setCreatorId("creator-1");
        creator.setAccount(account);

        series = new Series();
        series.setSeriesId(seriesId);
        series.setCreator(creator);
        series.setTitle("Sample Series");
        series.setContentType(ContentType.VIDEO);
        series.setStatus(SeriesStatus.DRAFT);

        season = new Season();
        season.setSeasonId(seasonId);
        season.setSeries(series);
        season.setCreatorId("creator-1");
        season.setSeasonNumber(1);
        season.setTitle("Season 1");
        season.setStatus(SeasonStatus.DRAFT);

        episode = new Episode();
        episode.setEpisodeId(episodeId);
        episode.setSeason(season);
        episode.setCreatorId("creator-1");
        episode.setEpisodeNumber(1);
        episode.setTitle("Pilot Episode");
        episode.setContentType(ContentType.VIDEO);
        episode.setStatus(EpisodeStatus.DRAFT);
        episode.setUnlockType(EpisodeUnlockType.FREE);
        episode.setPriceVnd(0L);

        episodeRequestDto = new EpisodeRequestDto();
        episodeRequestDto.setTitle("Pilot Episode");
        episodeRequestDto.setDescription("First episode description");
        episodeRequestDto.setContentType(ContentType.VIDEO);
    }

    // =========================================================================
    // Ep_001_CreateEpisode Tests
    // =========================================================================

    @Test
    @DisplayName("Ep_001 UTCID01 (Normal): Tạo tập đầu tiên thành công với episodeNumber = 1 và DRAFT/FREE")
    void createEpisode_FirstEpisode_Success_AutoAssignsNumber1() {
        when(seasonService.findActiveEntity(seasonId)).thenReturn(season);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(episodeRepository.findMaxEpisodeNumberBySeasonId(seasonId)).thenReturn(0);
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> {
            Episode ep = invocation.getArgument(0);
            ep.setEpisodeId(episodeId);
            return ep;
        });

        EpisodeResponseDto response = episodeService.create(seasonId, episodeRequestDto, accountId);

        assertNotNull(response);
        assertEquals(episodeId, response.getEpisodeId());
        assertEquals(1, response.getEpisodeNumber());
        assertEquals("Pilot Episode", response.getTitle());
        assertEquals(EpisodeStatus.DRAFT, response.getStatus());
        assertEquals(EpisodeUnlockType.FREE, response.getUnlockType());
        assertEquals(0L, response.getPriceVnd());

        verify(contentAuditLogger).logAction("Episode", episodeId, "CREATE", accountId, "creator-1");
    }

    @Test
    @DisplayName("Ep_001 UTCID02 (Normal): Tạo tập tiếp theo thành công (episodeNumber tự tăng lên 6)")
    void createEpisode_SubsequentEpisode_AutoIncrementsNumber() {
        when(seasonService.findActiveEntity(seasonId)).thenReturn(season);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(episodeRepository.findMaxEpisodeNumberBySeasonId(seasonId)).thenReturn(5);
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> {
            Episode ep = invocation.getArgument(0);
            ep.setEpisodeId("episode-6");
            return ep;
        });

        episodeRequestDto.setTitle("Episode 6");
        EpisodeResponseDto response = episodeService.create(seasonId, episodeRequestDto, accountId);

        assertNotNull(response);
        assertEquals(6, response.getEpisodeNumber());
        assertEquals("Episode 6", response.getTitle());
    }

    @Test
    @DisplayName("Ep_001 UTCID03 (Normal): Tạo tập COMIC thành công khi Series và Season là COMIC")
    void createEpisode_ComicEpisode_Success() {
        series.setContentType(ContentType.COMIC);
        episodeRequestDto.setContentType(ContentType.COMIC);

        when(seasonService.findActiveEntity(seasonId)).thenReturn(season);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(episodeRepository.findMaxEpisodeNumberBySeasonId(seasonId)).thenReturn(0);
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EpisodeResponseDto response = episodeService.create(seasonId, episodeRequestDto, accountId);

        assertNotNull(response);
        assertEquals(ContentType.COMIC, response.getContentType());
    }

    @Test
    @DisplayName("Ep_001 UTCID04 (Abnormal): Season không tồn tại ném lỗi NotFound")
    void createEpisode_SeasonNotFound_ThrowsException() {
        when(seasonService.findActiveEntity("non-existent"))
                .thenThrow(ContentModuleException.notFound("Season not found: non-existent"));

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.create("non-existent", episodeRequestDto, accountId));

        assertTrue(ex.getMessage().contains("Season not found"));
        verify(episodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ep_001 UTCID05 (Abnormal): Caller không sở hữu Season/Series ném lỗi Forbidden")
    void createEpisode_CallerNotOwner_ThrowsForbidden() {
        when(seasonService.findActiveEntity(seasonId)).thenReturn(season);
        doThrow(ContentModuleException.forbidden("User is not the owner"))
                .when(contentOwnershipService).assertCanManage(series, "unauthorized-user");

        assertThrows(ContentModuleException.class,
                () -> episodeService.create(seasonId, episodeRequestDto, "unauthorized-user"));

        verify(episodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ep_001 UTCID08 (Abnormal): contentType của Episode không khớp với Series ném BadRequest")
    void createEpisode_ContentTypeMismatch_ThrowsBadRequest() {
        series.setContentType(ContentType.VIDEO);
        episodeRequestDto.setContentType(ContentType.COMIC);

        when(seasonService.findActiveEntity(seasonId)).thenReturn(season);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.create(seasonId, episodeRequestDto, accountId));

        assertTrue(ex.getMessage().contains("Episode content type must match series content type"));
        verify(episodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ep_001 UTCID07 (Boundary): Tiêu đề episode đúng 150 ký tự (ngưỡng max)")
    void createEpisode_BoundaryMaxTitleLength_Success() {
        String maxTitle = "E".repeat(150);
        episodeRequestDto.setTitle(maxTitle);

        when(seasonService.findActiveEntity(seasonId)).thenReturn(season);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(episodeRepository.findMaxEpisodeNumberBySeasonId(seasonId)).thenReturn(0);
        when(episodeRepository.save(any(Episode.class))).thenAnswer(invocation -> invocation.getArgument(0));

        EpisodeResponseDto response = episodeService.create(seasonId, episodeRequestDto, accountId);

        assertNotNull(response);
        assertEquals(150, response.getTitle().length());
        assertEquals(maxTitle, response.getTitle());
    }

    // =========================================================================
    // Ep_002_UpdateUnlockSettings Tests
    // =========================================================================

    @Test
    @DisplayName("Ep_002 UTCID01 (Normal): Cập nhật unlockType sang FREE và priceVnd = 0")
    void updateUnlockSettings_Free_Success() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(accountRepository.findById(UUID.fromString(accountId))).thenReturn(Optional.of(account));
        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.FREE);
        unlockDto.setPriceVnd(0L);

        EpisodeResponseDto response = episodeService.updateUnlockSettings(episodeId, unlockDto, accountId);

        assertNotNull(response);
        assertEquals(EpisodeUnlockType.FREE, response.getUnlockType());
        assertEquals(0L, response.getPriceVnd());
        verify(contentAuditLogger).logAction("Episode", episodeId, "UPDATE_UNLOCK_SETTINGS", accountId, "creator-1");
    }

    @Test
    @DisplayName("Ep_002 UTCID02 (Normal): Cập nhật unlockType sang PAID với giá 5,000 VND hợp lệ")
    void updateUnlockSettings_Paid_Success() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(accountRepository.findById(UUID.fromString(accountId))).thenReturn(Optional.of(account));
        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.PAID);
        unlockDto.setPriceVnd(5000L);

        EpisodeResponseDto response = episodeService.updateUnlockSettings(episodeId, unlockDto, accountId);

        assertNotNull(response);
        assertEquals(EpisodeUnlockType.PAID, response.getUnlockType());
        assertEquals(5000L, response.getPriceVnd());
    }

    @Test
    @DisplayName("Ep_002 UTCID06 (Boundary): Cập nhật unlockType PAID với giá tối thiểu 1,000 VND")
    void updateUnlockSettings_PaidMinPrice_Boundary_Success() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(accountRepository.findById(UUID.fromString(accountId))).thenReturn(Optional.of(account));
        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.PAID);
        unlockDto.setPriceVnd(1000L);

        EpisodeResponseDto response = episodeService.updateUnlockSettings(episodeId, unlockDto, accountId);

        assertNotNull(response);
        assertEquals(1000L, response.getPriceVnd());
    }

    @Test
    @DisplayName("Ep_002 UTCID07 (Boundary): Cập nhật unlockType PAID với giá tối đa 99,999 VND (< 100,000)")
    void updateUnlockSettings_PaidMaxPrice_Boundary_Success() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(accountRepository.findById(UUID.fromString(accountId))).thenReturn(Optional.of(account));
        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.PAID);
        unlockDto.setPriceVnd(99999L);

        EpisodeResponseDto response = episodeService.updateUnlockSettings(episodeId, unlockDto, accountId);

        assertNotNull(response);
        assertEquals(99999L, response.getPriceVnd());
    }

    @Test
    @DisplayName("Ep_002 UTCID03 (Abnormal): Episode không tồn tại ném NotFound")
    void updateUnlockSettings_EpisodeNotFound_ThrowsException() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse("non-existent")).thenReturn(Optional.empty());

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.FREE);

        assertThrows(ContentModuleException.class,
                () -> episodeService.updateUnlockSettings("non-existent", unlockDto, accountId));
    }

    @Test
    @DisplayName("Ep_002 UTCID04 (Abnormal): Caller không sở hữu Episode ném Forbidden")
    void updateUnlockSettings_CallerNotOwner_ThrowsForbidden() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doThrow(ContentModuleException.forbidden("User is not the owner"))
                .when(contentOwnershipService).assertCanManage(episode, "other-user");

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.FREE);

        assertThrows(ContentModuleException.class,
                () -> episodeService.updateUnlockSettings(episodeId, unlockDto, "other-user"));
    }

    @Test
    @DisplayName("Ep_002 UTCID05 (Abnormal): Tài khoản không có Creator role ném Forbidden")
    void updateUnlockSettings_NonCreatorRole_ThrowsForbidden() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);

        Role userRole = new Role();
        userRole.setRoleId(1L); // Not 2L (Creator)
        account.setRole(userRole);
        when(accountRepository.findById(UUID.fromString(accountId))).thenReturn(Optional.of(account));

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.FREE);

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.updateUnlockSettings(episodeId, unlockDto, accountId));

        assertTrue(ex.getMessage().contains("Only creator accounts can update episode price settings"));
    }

    @Test
    @DisplayName("Ep_002 UTCID05 (Abnormal): Giá PAID <= 0 hoặc >= 100,000 VND ném BadRequest")
    void updateUnlockSettings_InvalidPrice_ThrowsBadRequest() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(accountRepository.findById(UUID.fromString(accountId))).thenReturn(Optional.of(account));

        EpisodeUnlockSettingsRequestDto unlockDto = new EpisodeUnlockSettingsRequestDto();
        unlockDto.setUnlockType(EpisodeUnlockType.PAID);
        unlockDto.setPriceVnd(0L); // invalid

        assertThrows(ContentModuleException.class,
                () -> episodeService.updateUnlockSettings(episodeId, unlockDto, accountId));

        unlockDto.setPriceVnd(100000L); // >= 100_000 is invalid
        assertThrows(ContentModuleException.class,
                () -> episodeService.updateUnlockSettings(episodeId, unlockDto, accountId));
    }

    // =========================================================================
    // Ep_003_PublishEpisode Tests
    // =========================================================================

    @Test
    @DisplayName("Ep_003 UTCID01 (Normal): Phát hành Episode từ DRAFT thành công khi media đã APPROVED và ACTIVE")
    void publishEpisode_FromDraft_Success_WithApprovedActiveMedia() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);

        when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(1L);
        when(mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                episodeId, ContentApprovalStatus.APPROVED)).thenReturn(false);
        when(mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                eq(episodeId), eq(MediaType.VIDEO), anyList())).thenReturn(1L);

        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeResponseDto response = episodeService.publish(episodeId, accountId);

        assertNotNull(response);
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
        assertNotNull(episode.getPublishedAt());
        assertNull(episode.getScheduledPublishAt());
        verify(contentAuditLogger).logAction("Episode", episodeId, "PUBLISH", accountId, "creator-1");
    }

    @Test
    @DisplayName("Ep_003 UTCID02 (Normal): Phát hành ngay Episode đang ở trạng thái SCHEDULED")
    void publishEpisode_FromScheduled_Success() {
        episode.setStatus(EpisodeStatus.SCHEDULED);
        episode.setScheduledPublishAt(LocalDateTime.now().plusDays(2));

        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);

        when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(1L);
        when(mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                episodeId, ContentApprovalStatus.APPROVED)).thenReturn(false);
        when(mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                eq(episodeId), eq(MediaType.VIDEO), anyList())).thenReturn(1L);
        when(episodeRepository.save(any(Episode.class))).thenReturn(episode);

        EpisodeResponseDto response = episodeService.publish(episodeId, accountId);

        assertNotNull(response);
        assertEquals(EpisodeStatus.PUBLISHED, episode.getStatus());
        assertNull(episode.getScheduledPublishAt());
    }

    @Test
    @DisplayName("Ep_003 UTCID03 (Abnormal): Episode không tồn tại ném NotFound")
    void publishEpisode_EpisodeNotFound_ThrowsException() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse("unknown")).thenReturn(Optional.empty());

        assertThrows(ContentModuleException.class, () -> episodeService.publish("unknown", accountId));
    }

    @Test
    @DisplayName("Ep_003 UTCID04 (Abnormal): Caller không sở hữu Episode ném Forbidden")
    void publishEpisode_CallerNotOwner_ThrowsForbidden() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doThrow(ContentModuleException.forbidden("User is not the owner"))
                .when(contentOwnershipService).assertCanManage(episode, "other-user");

        assertThrows(ContentModuleException.class, () -> episodeService.publish(episodeId, "other-user"));
    }

    @Test
    @DisplayName("Ep_003 UTCID07 (Boundary/Abnormal): Episode chưa có file Media ném BadRequest")
    void publishEpisode_NoMedia_ThrowsBadRequest() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(0L);

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.publish(episodeId, accountId));

        assertTrue(ex.getMessage().contains("Episode must have at least one media before publishing"));
        verify(episodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ep_003 UTCID07 (Boundary/Abnormal): Media chưa được duyệt APPROVED ném BadRequest")
    void publishEpisode_UnapprovedMedia_ThrowsBadRequest() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(1L);
        when(mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                episodeId, ContentApprovalStatus.APPROVED)).thenReturn(true);

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.publish(episodeId, accountId));

        assertTrue(ex.getMessage().contains("must have approval_status as APPROVED"));
        verify(episodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ep_003 UTCID07 (Boundary/Abnormal): Media chưa chuyển sang trạng thái ACTIVE/HLS_READY ném BadRequest")
    void publishEpisode_MediaNotReady_ThrowsBadRequest() {
        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(2L);
        when(mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                episodeId, ContentApprovalStatus.APPROVED)).thenReturn(false);
        when(mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                eq(episodeId), eq(MediaType.VIDEO), anyList())).thenReturn(1L); // only 1 of 2 is ready

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.publish(episodeId, accountId));

        assertTrue(ex.getMessage().contains("All media must be processed and ready"));
        verify(episodeRepository, never()).save(any());
    }

    @Test
    @DisplayName("Ep_003 UTCID06 (Abnormal): Parent Season hoặc Series bị DELETED ném BadRequest")
    void publishEpisode_ParentDeleted_ThrowsBadRequest() {
        season.setStatus(SeasonStatus.DELETED);

        when(episodeRepository.findByEpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(Optional.of(episode));
        doNothing().when(contentOwnershipService).assertCanManage(episode, accountId);
        when(mediaRepository.countByEpisode_EpisodeIdAndIsDeletedFalse(episodeId)).thenReturn(1L);
        when(mediaRepository.existsByEpisode_EpisodeIdAndApprovalStatusNotAndIsDeletedFalse(
                episodeId, ContentApprovalStatus.APPROVED)).thenReturn(false);
        when(mediaRepository.countByEpisode_EpisodeIdAndMediaTypeAndStatusInAndIsDeletedFalse(
                eq(episodeId), eq(MediaType.VIDEO), anyList())).thenReturn(1L);

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> episodeService.publish(episodeId, accountId));

        assertTrue(ex.getMessage().contains("Cannot publish an episode whose season or series is deleted"));
        verify(episodeRepository, never()).save(any());
    }
}
