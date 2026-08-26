package com.talex.server.services.series;

import com.talex.server.dtos.requests.series.SeasonRequestDto;
import com.talex.server.dtos.responses.series.SeasonResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.series.Season;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.series.SeasonStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.SeasonRepository;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.series.impls.ContentCascadeDeleteHelper;
import com.talex.server.services.series.impls.SeasonServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("SeasonService Unit Tests - Covering Season_001_CreateSeason")
class SeasonServiceTest {

    @Mock
    private SeasonRepository seasonRepository;

    @Mock
    private SeriesService seriesService;

    @Mock
    private ContentOwnershipService contentOwnershipService;

    @Mock
    private ContentAuditLogger contentAuditLogger;

    @Mock
    private ContentCascadeDeleteHelper contentCascadeDeleteHelper;

    @InjectMocks
    private SeasonServiceImpl seasonService;

    private Series series;
    private Creator creator;
    private Season season;
    private SeasonRequestDto requestDto;
    private final String accountId = "account-1";
    private final String seriesId = "series-1";
    private final String seasonId = "season-1";

    @BeforeEach
    void setUp() {
        creator = new Creator();
        creator.setCreatorId("creator-1");

        series = new Series();
        series.setSeriesId(seriesId);
        series.setTitle("Sample Series");
        series.setCreator(creator);
        series.setStatus(SeriesStatus.DRAFT);

        season = new Season();
        season.setSeasonId(seasonId);
        season.setSeries(series);
        season.setCreatorId("creator-1");
        season.setSeasonNumber(1);
        season.setTitle("Season 1");
        season.setStatus(SeasonStatus.DRAFT);

        requestDto = new SeasonRequestDto();
        requestDto.setTitle("Season 1");
        requestDto.setDescription("Description of season 1");
    }

    // =========================================================================
    // Season_001_CreateSeason Tests
    // =========================================================================

    @Test
    @DisplayName("UTCID01 (Normal): Tạo Season đầu tiên thành công với seasonNumber = 1")
    void createSeason_FirstSeason_AutoAssignsNumber1() {
        when(seriesService.findActiveEntity(seriesId)).thenReturn(series);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(seasonRepository.findMaxSeasonNumberBySeriesId(seriesId)).thenReturn(0);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> {
            Season s = invocation.getArgument(0);
            s.setSeasonId(seasonId);
            return s;
        });

        SeasonResponseDto response = seasonService.create(seriesId, requestDto, accountId);

        assertNotNull(response);
        assertEquals(seasonId, response.getSeasonId());
        assertEquals(1, response.getSeasonNumber());
        assertEquals("Season 1", response.getTitle());
        assertEquals(SeasonStatus.DRAFT, response.getStatus());

        verify(contentAuditLogger).logAction("Season", seasonId, "CREATE", accountId, "creator-1");
    }

    @Test
    @DisplayName("UTCID02 (Normal): Tạo Season tiếp theo thành công (seasonNumber tự tăng lên 3)")
    void createSeason_SubsequentSeason_AutoIncrementsNumber() {
        when(seriesService.findActiveEntity(seriesId)).thenReturn(series);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(seasonRepository.findMaxSeasonNumberBySeriesId(seriesId)).thenReturn(2);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> {
            Season s = invocation.getArgument(0);
            s.setSeasonId("season-3");
            return s;
        });

        requestDto.setTitle("Season 3");
        SeasonResponseDto response = seasonService.create(seriesId, requestDto, accountId);

        assertNotNull(response);
        assertEquals(3, response.getSeasonNumber());
        assertEquals("Season 3", response.getTitle());
    }

    @Test
    @DisplayName("UTCID03 (Abnormal): Series không tồn tại ném lỗi NotFound")
    void createSeason_SeriesNotFound_ThrowsException() {
        when(seriesService.findActiveEntity("non-existent"))
                .thenThrow(ContentModuleException.notFound("Series not found: non-existent"));

        ContentModuleException ex = assertThrows(ContentModuleException.class,
                () -> seasonService.create("non-existent", requestDto, accountId));

        assertTrue(ex.getMessage().contains("Series not found"));
        verify(seasonRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID04 (Abnormal): Caller không phải owner của Series ném lỗi Forbidden")
    void createSeason_CallerNotOwner_ThrowsForbidden() {
        when(seriesService.findActiveEntity(seriesId)).thenReturn(series);
        doThrow(ContentModuleException.forbidden("User is not the owner"))
                .when(contentOwnershipService).assertCanManage(series, "unauthorized-user");

        assertThrows(ContentModuleException.class,
                () -> seasonService.create(seriesId, requestDto, "unauthorized-user"));

        verify(seasonRepository, never()).save(any());
    }

    @Test
    @DisplayName("UTCID06 (Boundary): Tiêu đề season đúng 100 ký tự (ngưỡng max)")
    void createSeason_TitleExactly100Chars_Boundary() {
        String longTitle = "A".repeat(100);
        requestDto.setTitle(longTitle);

        when(seriesService.findActiveEntity(seriesId)).thenReturn(series);
        doNothing().when(contentOwnershipService).assertCanManage(series, accountId);
        when(seasonRepository.findMaxSeasonNumberBySeriesId(seriesId)).thenReturn(0);
        when(seasonRepository.save(any(Season.class))).thenAnswer(invocation -> invocation.getArgument(0));

        SeasonResponseDto response = seasonService.create(seriesId, requestDto, accountId);

        assertNotNull(response);
        assertEquals(100, response.getTitle().length());
        assertEquals(longTitle, response.getTitle());
    }

    // =========================================================================
    // Other Season Operations (getById, update, hide, unhide, delete)
    // =========================================================================

    @Test
    void getById_Success() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse(seasonId)).thenReturn(Optional.of(season));
        doNothing().when(contentOwnershipService).assertCanView(season, accountId);

        SeasonResponseDto response = seasonService.getById(seasonId, accountId);

        assertNotNull(response);
        assertEquals(seasonId, response.getSeasonId());
    }

    @Test
    void getById_NotFound() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse("unknown")).thenReturn(Optional.empty());

        assertThrows(ContentModuleException.class, () -> seasonService.getById("unknown", accountId));
    }

    @Test
    void update_Success() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse(seasonId)).thenReturn(Optional.of(season));
        doNothing().when(contentOwnershipService).assertCanManage(season, accountId);
        when(seasonRepository.save(any(Season.class))).thenReturn(season);

        SeasonRequestDto updateDto = new SeasonRequestDto();
        updateDto.setTitle("Updated Season Title");
        updateDto.setDescription("Updated Desc");

        SeasonResponseDto response = seasonService.update(seasonId, updateDto, accountId);

        assertNotNull(response);
        assertEquals("Updated Season Title", response.getTitle());
        verify(contentAuditLogger).logAction("Season", seasonId, "UPDATE", accountId, "creator-1");
    }

    @Test
    void update_ThrowsOnScheduledStatus() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse(seasonId)).thenReturn(Optional.of(season));
        doNothing().when(contentOwnershipService).assertCanManage(season, accountId);

        SeasonRequestDto updateDto = new SeasonRequestDto();
        updateDto.setStatus(SeasonStatus.SCHEDULED);

        assertThrows(ContentModuleException.class, () -> seasonService.update(seasonId, updateDto, accountId));
    }

    @Test
    void hide_Success() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse(seasonId)).thenReturn(Optional.of(season));
        doNothing().when(contentOwnershipService).assertCanManage(season, accountId);
        when(seasonRepository.save(any(Season.class))).thenReturn(season);

        SeasonResponseDto response = seasonService.hide(seasonId, accountId);

        assertNotNull(response);
        assertEquals(SeasonStatus.HIDDEN, season.getStatus());
        verify(contentAuditLogger).logAction("Season", seasonId, "HIDE", accountId, "creator-1");
    }

    @Test
    void unhide_Success() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse(seasonId)).thenReturn(Optional.of(season));
        doNothing().when(contentOwnershipService).assertCanManage(season, accountId);
        when(seasonRepository.save(any(Season.class))).thenReturn(season);

        SeasonResponseDto response = seasonService.unhide(seasonId, accountId);

        assertNotNull(response);
        assertEquals(SeasonStatus.PUBLISHED, season.getStatus());
        verify(contentAuditLogger).logAction("Season", seasonId, "UNHIDE", accountId, "creator-1");
    }

    @Test
    void delete_Success() {
        when(seasonRepository.findBySeasonIdAndIsDeletedFalse(seasonId)).thenReturn(Optional.of(season));
        doNothing().when(contentOwnershipService).assertCanManage(season, accountId);
        when(seasonRepository.save(any(Season.class))).thenReturn(season);

        seasonService.delete(seasonId, accountId);

        assertEquals(SeasonStatus.DELETED, season.getStatus());
        assertTrue(season.getIsDeleted());
        verify(contentCascadeDeleteHelper).cascadeDeleteSeasonEpisodes(seasonId, accountId);
        verify(contentAuditLogger).logAction("Season", seasonId, "DELETE", accountId, "creator-1");
    }
}
