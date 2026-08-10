package com.talex.server.services.series;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.analytic.SeriesLogResponseDto;
import com.talex.server.dtos.recommend.SeriesCardResponseDto;
import com.talex.server.dtos.requests.series.SeriesRequestDto;
import com.talex.server.dtos.requests.series.SeriesSearchCriteria;
import com.talex.server.dtos.responses.series.CategoryResponseDto;
import com.talex.server.dtos.responses.series.SeriesResponseDto;
import com.talex.server.dtos.responses.series.TagResponseDto;
import com.talex.server.entities.auth.Account;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.series.*;
import com.talex.server.enums.series.CategoryStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.enums.series.TagStatus;
import com.talex.server.enums.series.ContentType;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.*;
import com.talex.server.repositories.series.projections.SeriesCardProjection;
import com.talex.server.repositories.series.projections.SeriesWithAvatarProjection;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.mongo.ISeriesFeatureService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.talex.server.services.series.impls.SeriesServiceImpl;

@ExtendWith(MockitoExtension.class)
class SeriesServiceTest {

    @Mock
    private SeriesRepository seriesRepository;
    @Mock
    private SeriesCategoryRepository seriesCategoryRepository;
    @Mock
    private SeriesTagRepository seriesTagRepository;
    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private TagRepository tagRepository;
    @Mock
    private SeriesLogRepository seriesLogRepository;
    @Mock
    private CategoryService categoryService;
    @Mock
    private TagService tagService;
    @Mock
    private ContentOwnershipService contentOwnershipService;
    @Mock
    private ISeriesFeatureService seriesFeatureService;
    @Mock
    private CreatorService creatorService;
    @Mock
    private SeasonRepository seasonRepository;
    @Mock
    private ContentAuditLogger contentAuditLogger;

    @InjectMocks
    private SeriesServiceImpl seriesService;

    private UUID accountId;
    private String accountIdStr;
    private Creator creator;
    private Series series;
    private SeriesRequestDto requestDto;

    @BeforeEach
    void setUp() {
        accountId = UUID.randomUUID();
        accountIdStr = accountId.toString();

        Account account = new Account();
        account.setAccountId(accountId);
        account.setTotalFollowersBy(10L);

        creator = new Creator();
        creator.setCreatorId("creator-1");
        creator.setAccount(account);

        series = new Series();
        series.setSeriesId("series-1");
        series.setCreator(creator);
        series.setTitle("Test Series");
        series.setDescription("Desc");
        series.setStatus(SeriesStatus.DRAFT);
        series.setLanguage("vi");

        requestDto = new SeriesRequestDto();
        requestDto.setTitle("New Title");
        requestDto.setDescription("New Desc");
        requestDto.setCategoryIds(List.of("cat-1"));
        requestDto.setTagIds(List.of("tag-1"));
    }

    // --- create ---

    @Test
    void create_Success() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        when(seriesRepository.save(any(Series.class))).thenAnswer(inv -> {
            Series s = inv.getArgument(0);
            s.setSeriesId("new-series");
            return s;
        });

        Category cat = new Category();
        cat.setCategoryId("cat-1");
        cat.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of(cat));
        when(seriesCategoryRepository.findBySeries_SeriesId(any())).thenReturn(List.of());

        Tag tag = new Tag();
        tag.setTagId("tag-1");
        tag.setStatus(TagStatus.ACTIVE);
        when(tagRepository.findAllByTagIdInAndIsDeletedFalse(any())).thenReturn(List.of(tag));
        when(seriesTagRepository.findBySeries_SeriesId(any())).thenReturn(List.of());

        SeriesResponseDto response = seriesService.create(requestDto, accountId);

        assertNotNull(response);
        assertEquals("New Title", response.getTitle());
        verify(seasonRepository).save(any(Season.class));
        verify(seriesFeatureService).saveSeriesMetadata(any(), any(), any());
        verify(contentAuditLogger, times(2)).logAction(anyString(), any(), eq("CREATE"), eq(accountIdStr), eq("creator-1"));
    }

    @Test
    void create_CategoryNotFound() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of());

        ContentModuleException ex = assertThrows(ContentModuleException.class, () -> seriesService.create(requestDto, accountId));
        assertTrue(ex.getMessage().contains("Category not found"));
    }

    @Test
    void create_CategoryNotActive() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        Category cat = new Category();
        cat.setCategoryId("cat-1");
        cat.setStatus(CategoryStatus.INACTIVE);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of(cat));

        ContentModuleException ex = assertThrows(ContentModuleException.class, () -> seriesService.create(requestDto, accountId));
        assertTrue(ex.getMessage().contains("Category is not active"));
    }

    @Test
    void create_TagNotFound() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        
        Category cat = new Category();
        cat.setCategoryId("cat-1");
        cat.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of(cat));
        
        when(tagRepository.findAllByTagIdInAndIsDeletedFalse(any())).thenReturn(List.of());

        ContentModuleException ex = assertThrows(ContentModuleException.class, () -> seriesService.create(requestDto, accountId));
        assertTrue(ex.getMessage().contains("Tag not found"));
    }

    @Test
    void create_TagNotActive() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        
        Category cat = new Category();
        cat.setCategoryId("cat-1");
        cat.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of(cat));
        
        Tag tag = new Tag();
        tag.setTagId("tag-1");
        tag.setStatus(TagStatus.INACTIVE);
        when(tagRepository.findAllByTagIdInAndIsDeletedFalse(any())).thenReturn(List.of(tag));

        ContentModuleException ex = assertThrows(ContentModuleException.class, () -> seriesService.create(requestDto, accountId));
        assertTrue(ex.getMessage().contains("Tag is not active"));
    }

    @Test
    void create_NullIds() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        requestDto.setCategoryIds(null);
        requestDto.setTagIds(null);

        SeriesResponseDto response = seriesService.create(requestDto, accountId);
        assertNotNull(response);
    }

    // --- getById ---

    @Test
    void getById_Success() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        doNothing().when(contentOwnershipService).assertCanView(series, accountIdStr);

        SeriesResponseDto response = seriesService.getById("series-1", accountIdStr);
        assertEquals("series-1", response.getSeriesId());
    }

    @Test
    void getById_NotFound() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.empty());

        assertThrows(ContentModuleException.class, () -> seriesService.getById("series-1", accountIdStr));
    }

    // --- getPublicById ---

    @Test
    void getPublicById_Success_Published() {
        series.setStatus(SeriesStatus.PUBLISHED);
        SeriesWithAvatarProjection projection = mock(SeriesWithAvatarProjection.class);
        when(projection.getSeries()).thenReturn(series);
        when(projection.getAvatarUrl()).thenReturn("avatar.png");
        when(projection.getCreatorFullName()).thenReturn("Full Name");

        when(seriesRepository.findActiveSeriesWithAvatarById("series-1")).thenReturn(Optional.of(projection));

        SeriesResponseDto response = seriesService.getPublicById("series-1");
        assertEquals("series-1", response.getSeriesId());
        assertEquals("avatar.png", response.getCreatorAvatar());
        assertEquals("Full Name", response.getCreatorName());
    }

    @Test
    void getPublicById_Success_Scheduled() {
        series.setStatus(SeriesStatus.SCHEDULED);
        SeriesWithAvatarProjection projection = mock(SeriesWithAvatarProjection.class);
        when(projection.getSeries()).thenReturn(series);
        when(projection.getCreatorFullName()).thenReturn("  ");
        when(projection.getCreatorUsername()).thenReturn("username");

        when(seriesRepository.findActiveSeriesWithAvatarById("series-1")).thenReturn(Optional.of(projection));

        SeriesResponseDto response = seriesService.getPublicById("series-1");
        assertEquals("series-1", response.getSeriesId());
        assertEquals("username", response.getCreatorName());
    }

    @Test
    void getPublicById_NotFound() {
        when(seriesRepository.findActiveSeriesWithAvatarById("series-1")).thenReturn(Optional.empty());
        assertThrows(ContentModuleException.class, () -> seriesService.getPublicById("series-1"));
    }

    @Test
    void getPublicById_InvalidStatus() {
        series.setStatus(SeriesStatus.DRAFT);
        SeriesWithAvatarProjection projection = mock(SeriesWithAvatarProjection.class);
        when(projection.getSeries()).thenReturn(series);

        when(seriesRepository.findActiveSeriesWithAvatarById("series-1")).thenReturn(Optional.of(projection));
        assertThrows(ContentModuleException.class, () -> seriesService.getPublicById("series-1"));
    }

    // --- list ---

    @Test
    void list_Success() {
        Page<Series> page = new PageImpl<>(List.of(series), PageRequest.of(0, 10), 1);
        when(seriesRepository.findAllByIsDeletedFalse(any(Pageable.class))).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.list(1, 10);
        assertEquals(1, response.getContent().size());
        assertEquals(1, response.getTotalElements());
    }

    @Test
    void list_Empty() {
        Page<Series> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);
        when(seriesRepository.findAllByIsDeletedFalse(any(Pageable.class))).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.list(1, 10);
        assertEquals(0, response.getContent().size());
    }

    // --- listByCreator ---

    @Test
    void listByCreator_WithStatuses() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        Page<Series> page = new PageImpl<>(List.of(series));
        when(seriesRepository.findAllByCreator_CreatorIdAndStatusInAndIsDeletedFalse(eq("creator-1"), any(), any(Pageable.class))).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.listByCreator(accountId, List.of(SeriesStatus.PUBLISHED), 1, 10);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void listByCreator_WithoutStatuses() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        Page<Series> page = new PageImpl<>(List.of(series));
        when(seriesRepository.findAllByCreator_CreatorIdAndIsDeletedFalse(eq("creator-1"), any(Pageable.class))).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.listByCreator(accountId, null, 1, 10);
        assertEquals(1, response.getContent().size());
    }

    // --- listPublic ---

    @Test
    void listPublic_Success() {
        SeriesWithAvatarProjection projection = mock(SeriesWithAvatarProjection.class);
        when(projection.getSeries()).thenReturn(series);
        Page<SeriesWithAvatarProjection> page = new PageImpl<>(List.of(projection));
        when(seriesRepository.findPublicSeriesWithAvatar(any(), any())).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.listPublic(1, 10);
        assertEquals(1, response.getContent().size());
    }

    @Test
    void listByCreator_EmptyStatuses() {
        when(creatorService.getEntityByAccountId(accountId)).thenReturn(creator);
        Page<Series> page = new PageImpl<>(List.of(series));
        when(seriesRepository.findAllByCreator_CreatorIdAndIsDeletedFalse(eq("creator-1"), any(Pageable.class))).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.listByCreator(accountId, List.of(), 1, 10);
        assertEquals(1, response.getContent().size());
    }
    
    @Test
    void listPublic_Empty() {
        Page<SeriesWithAvatarProjection> page = new PageImpl<>(List.of());
        when(seriesRepository.findPublicSeriesWithAvatar(any(), any())).thenReturn(page);

        BasePageResponse<SeriesResponseDto> response = seriesService.listPublic(1, 10);
        assertEquals(0, response.getContent().size());
    }

    // --- searchPublic ---
    // searchPublicSeries đổi sang native SQL (unaccent + multi-field + ranking) — signature
    // đổi: statuses/contentType giờ là String (không phải enum), thêm param sortBy riêng,
    // trả Page<SeriesCardProjection> (interface projection, không phải constructor-projection
    // DTO trực tiếp) vì native query không dùng được "new ...DTO(...)". Test unit chỉ verify
    // được tầng mapping/normalize ở service — hành vi SQL thật (unaccent, EXISTS, ranking)
    // không kiểm được bằng repo mock, phải verify qua manual/FE test.

    @Test
    void searchPublic_MapsPageAndDefaultsToPopular() {
        SeriesCardProjection projection = mock(SeriesCardProjection.class);
        when(projection.getSeriesId()).thenReturn("s1");
        when(projection.getTitle()).thenReturn("A");
        when(projection.getTotalViews()).thenReturn(100L);
        Page<SeriesCardProjection> page = new PageImpl<>(List.of(projection), PageRequest.of(0, 12), 1);
        when(seriesRepository.searchPublicSeries(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        SeriesSearchCriteria criteria = new SeriesSearchCriteria(null, null, null, null, null, null, null);
        BasePageResponse<SeriesCardResponseDto> response =
                seriesService.searchPublic(criteria, "popular", 1, 12);

        assertEquals(1, response.getContent().size());
        assertEquals("s1", response.getContent().get(0).getSeriesId());
        assertEquals("A", response.getContent().get(0).getTitle());
        assertEquals(100L, response.getContent().get(0).getTotalViews());
        assertEquals(1, response.getTotalElements());
        assertEquals(1, response.getPageNumber());
    }

    @Test
    void searchPublic_NormalizesKeywordAndForwardsFilters() {
        Page<SeriesCardProjection> page = new PageImpl<>(List.of());
        when(seriesRepository.searchPublicSeries(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        SeriesSearchCriteria criteria =
                new SeriesSearchCriteria("  Naruto ", ContentType.VIDEO, "cat", "tag", 2020, 2024, 500L);

        seriesService.searchPublic(criteria, "newest", 2, 8);

        org.mockito.ArgumentCaptor<String> keywordCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        org.mockito.ArgumentCaptor<Pageable> pageableCaptor = org.mockito.ArgumentCaptor.forClass(Pageable.class);
        verify(seriesRepository).searchPublicSeries(any(), keywordCaptor.capture(), eq("VIDEO"),
                eq("cat"), eq("tag"), eq(2020), eq(2024), eq(500L), eq("newest"), pageableCaptor.capture());
        assertEquals("%naruto%", keywordCaptor.getValue());
        // Sort nằm cứng trong SQL native, Pageable chỉ mang page/size — không set Sort riêng.
        assertTrue(pageableCaptor.getValue().getSort().isUnsorted());
    }

    @Test
    void searchPublic_StripsVietnameseAccentsInKeyword() {
        Page<SeriesCardProjection> page = new PageImpl<>(List.of());
        when(seriesRepository.searchPublicSeries(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        SeriesSearchCriteria criteria =
                new SeriesSearchCriteria("Vua Sư Tử", null, null, null, null, null, null);

        seriesService.searchPublic(criteria, "popular", 1, 12);

        org.mockito.ArgumentCaptor<String> keywordCaptor = org.mockito.ArgumentCaptor.forClass(String.class);
        verify(seriesRepository).searchPublicSeries(any(), keywordCaptor.capture(), any(), any(), any(),
                any(), any(), any(), any(), any(Pageable.class));
        assertEquals("%vua su tu%", keywordCaptor.getValue());
    }

    @Test
    void searchPublic_DefaultsInvalidSortByToPopular() {
        Page<SeriesCardProjection> page = new PageImpl<>(List.of());
        when(seriesRepository.searchPublicSeries(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);
        SeriesSearchCriteria criteria = new SeriesSearchCriteria(null, null, null, null, null, null, null);

        seriesService.searchPublic(criteria, "xyz-invalid", 1, 12);

        verify(seriesRepository).searchPublicSeries(any(), any(), any(), any(), any(),
                any(), any(), any(), eq("popular"), any(Pageable.class));
    }

    // --- update ---

    @Test
    void update_Success() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        doNothing().when(contentOwnershipService).assertCanManage(series, accountIdStr);
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        Category cat = new Category();
        cat.setCategoryId("cat-1");
        cat.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of(cat));
        
        SeriesCategory existingSc = new SeriesCategory(series, cat);
        existingSc.setIsDeleted(true);
        when(seriesCategoryRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSc));

        Tag tag = new Tag();
        tag.setTagId("tag-1");
        tag.setStatus(TagStatus.ACTIVE);
        when(tagRepository.findAllByTagIdInAndIsDeletedFalse(any())).thenReturn(List.of(tag));
        
        SeriesTag existingSt = new SeriesTag(series, tag);
        existingSt.setIsDeleted(true);
        when(seriesTagRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSt));

        requestDto.setStatus(SeriesStatus.HIDDEN);
        SeriesResponseDto response = seriesService.update("series-1", requestDto, accountIdStr);
        assertEquals(SeriesStatus.HIDDEN, response.getStatus());
        verify(seriesFeatureService).saveSeriesMetadata(any(), any(), any());
        verify(contentAuditLogger).logAction("Series", "series-1", "UPDATE", accountIdStr, "creator-1");
    }

    @Test
    void update_RemoveExistingRelations() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        
        Category cat = new Category();
        cat.setCategoryId("cat-old");
        SeriesCategory existingSc = new SeriesCategory(series, cat);
        existingSc.setIsDeleted(false);
        when(seriesCategoryRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSc));
        
        Tag tag = new Tag();
        tag.setTagId("tag-old");
        SeriesTag existingSt = new SeriesTag(series, tag);
        existingSt.setIsDeleted(false);
        when(seriesTagRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSt));
        
        requestDto.setCategoryIds(List.of());
        requestDto.setTagIds(List.of());

        seriesService.update("series-1", requestDto, accountIdStr);
        assertTrue(existingSc.getIsDeleted());
        assertTrue(existingSt.getIsDeleted());
    }

    @Test
    void update_ThrowOnScheduled() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        requestDto.setStatus(SeriesStatus.SCHEDULED);

        assertThrows(ContentModuleException.class, () -> seriesService.update("series-1", requestDto, accountIdStr));
    }

    // --- hide, unhide, forceHide, forceUnhide, delete ---

    @Test
    void hide_Success() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        seriesService.hide("series-1", accountIdStr);
        assertEquals(SeriesStatus.HIDDEN, series.getStatus());
        verify(contentAuditLogger).logAction("Series", "series-1", "HIDE", accountIdStr, "creator-1");
    }

    @Test
    void unhide_Success() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        seriesService.unhide("series-1", accountIdStr);
        assertEquals(SeriesStatus.PUBLISHED, series.getStatus());
        verify(contentAuditLogger).logAction("Series", "series-1", "UNHIDE", accountIdStr, "creator-1");
    }

    @Test
    void forceHide_Success() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        seriesService.forceHide("series-1", accountIdStr);
        assertEquals(SeriesStatus.FORCE_HIDDEN, series.getStatus());
        verify(contentAuditLogger).logAction("Series", "series-1", "FORCE_HIDE", accountIdStr, "creator-1");
    }

    @Test
    void forceUnhide_Success() {
        series.setStatus(SeriesStatus.FORCE_HIDDEN);
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);

        seriesService.forceUnhide("series-1", accountIdStr);
        assertEquals(SeriesStatus.HIDDEN, series.getStatus());
        verify(contentAuditLogger).logAction("Series", "series-1", "FORCE_UNHIDE", accountIdStr, "creator-1");
    }
    
    @Test
    void forceUnhide_NotForceHidden() {
        series.setStatus(SeriesStatus.PUBLISHED);
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        
        assertThrows(ContentModuleException.class, () -> seriesService.forceUnhide("series-1", accountIdStr));
    }

    @Test
    void delete_Success() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));

        seriesService.delete("series-1", accountIdStr);
        assertEquals(SeriesStatus.DELETED, series.getStatus());
        assertTrue(series.getIsDeleted());
        verify(contentAuditLogger).logAction("Series", "series-1", "DELETE", accountIdStr, "creator-1");
    }

    // --- getSeriesLogs ---

    @Test
    void getSeriesLogs_Success() {
        SeriesLog log = new SeriesLog();
        log.setSeriesLogId("log-1");
        log.setSeries(series);
        log.setHourBucket(LocalDateTime.now());
        when(seriesLogRepository.findBySeriesSeriesIdAndHourBucketBetweenOrderByHourBucketAsc(anyString(), any(), any())).thenReturn(List.of(log));

        List<SeriesLogResponseDto> response = seriesService.getSeriesLogs("series-1", LocalDateTime.now(), LocalDateTime.now(), accountIdStr);
        assertEquals(1, response.size());
    }
    
    // --- missed branches ---
    
    @Test
    void update_StatusUnchangedOrNull() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        series.setStatus(SeriesStatus.DRAFT);
        
        requestDto.setCategoryIds(null);
        requestDto.setTagIds(null);

        requestDto.setStatus(null); // null case
        seriesService.update("series-1", requestDto, accountIdStr);
        assertEquals(SeriesStatus.DRAFT, series.getStatus());
        
        requestDto.setStatus(SeriesStatus.DRAFT); // unchanged case
        seriesService.update("series-1", requestDto, accountIdStr);
        assertEquals(SeriesStatus.DRAFT, series.getStatus());
    }
    
    @Test
    void syncCategories_ExistingNotDeletedAndInRequestedIds() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        
        Category cat = new Category();
        cat.setCategoryId("cat-1");
        cat.setStatus(CategoryStatus.ACTIVE);
        when(categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(any())).thenReturn(List.of(cat));
        
        SeriesCategory existingSc = new SeriesCategory(series, cat);
        existingSc.setIsDeleted(false); // not deleted, and we request it
        when(seriesCategoryRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSc));
        
        Tag tag = new Tag();
        tag.setTagId("tag-1");
        tag.setStatus(TagStatus.ACTIVE);
        when(tagRepository.findAllByTagIdInAndIsDeletedFalse(any())).thenReturn(List.of(tag));
        
        SeriesTag existingSt = new SeriesTag(series, tag);
        existingSt.setIsDeleted(false); // not deleted, and we request it
        when(seriesTagRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSt));
        
        // Add a blank id to hit the filter in cleanIds
        requestDto.setCategoryIds(Arrays.asList("cat-1", null, "   "));
        requestDto.setTagIds(Arrays.asList("tag-1", null, "   "));

        seriesService.update("series-1", requestDto, accountIdStr);
        assertFalse(existingSc.getIsDeleted());
        assertFalse(existingSt.getIsDeleted());
    }
    
    @Test
    void syncCategories_ExistingDeletedButNotRequested() {
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        when(seriesRepository.save(any(Series.class))).thenReturn(series);
        
        Category cat = new Category();
        cat.setCategoryId("cat-old");
        SeriesCategory existingSc = new SeriesCategory(series, cat);
        existingSc.setIsDeleted(true); // already deleted, shouldn't softDelete again
        when(seriesCategoryRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSc));
        
        Tag tag = new Tag();
        tag.setTagId("tag-old");
        SeriesTag existingSt = new SeriesTag(series, tag);
        existingSt.setIsDeleted(true); // already deleted, shouldn't softDelete again
        when(seriesTagRepository.findBySeries_SeriesId(any())).thenReturn(List.of(existingSt));
        
        requestDto.setCategoryIds(List.of());
        requestDto.setTagIds(List.of());

        seriesService.update("series-1", requestDto, accountIdStr);
        assertTrue(existingSc.getIsDeleted());
        assertTrue(existingSt.getIsDeleted());
    }
    
    @Test
    void getPublicById_NullCreatorFullName() {
        series.setStatus(SeriesStatus.PUBLISHED);
        SeriesWithAvatarProjection projection = mock(SeriesWithAvatarProjection.class);
        when(projection.getSeries()).thenReturn(series);
        when(projection.getAvatarUrl()).thenReturn("avatar.png");
        when(projection.getCreatorFullName()).thenReturn(null); // null full name
        when(projection.getCreatorUsername()).thenReturn("fallbackUser");

        when(seriesRepository.findActiveSeriesWithAvatarById("series-1")).thenReturn(Optional.of(projection));

        SeriesResponseDto response = seriesService.getPublicById("series-1");
        assertEquals("fallbackUser", response.getCreatorName());
    }

    // --- helpers tests (to hit lines) ---
    
    @Test
    void loadCategoryAndTagResponses() {
        // Trigger via list or getById
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        
        SeriesCategoryId scId = new SeriesCategoryId("series-1", "cat-1");
        Category cat = new Category();
        cat.setCategoryId("cat-1");
        SeriesCategory sc = new SeriesCategory();
        sc.setId(scId);
        sc.setCategory(cat);
        when(seriesCategoryRepository.findBySeries_SeriesIdInAndIsDeletedFalse(any())).thenReturn(List.of(sc));
        
        SeriesTagId stId = new SeriesTagId("series-1", "tag-1");
        Tag tag = new Tag();
        tag.setTagId("tag-1");
        SeriesTag st = new SeriesTag();
        st.setId(stId);
        st.setTag(tag);
        when(seriesTagRepository.findBySeries_SeriesIdInAndIsDeletedFalse(any())).thenReturn(List.of(st));
        
        when(categoryService.toResponse(any())).thenReturn(new CategoryResponseDto());
        when(tagService.toResponse(any())).thenReturn(new TagResponseDto());

        SeriesResponseDto dto = seriesService.getById("series-1", accountIdStr);
        assertEquals(1, dto.getCategories().size());
        assertEquals(1, dto.getTags().size());
    }

    @Test
    void findPublicEntity_Success_Published() {
        series.setStatus(SeriesStatus.PUBLISHED);
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        Series result = seriesService.findPublicEntity("series-1");
        assertNotNull(result);
        assertEquals(SeriesStatus.PUBLISHED, result.getStatus());
    }

    @Test
    void findPublicEntity_Success_Scheduled() {
        series.setStatus(SeriesStatus.SCHEDULED);
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        Series result = seriesService.findPublicEntity("series-1");
        assertNotNull(result);
        assertEquals(SeriesStatus.SCHEDULED, result.getStatus());
    }

    @Test
    void findPublicEntity_NotFound_Draft() {
        series.setStatus(SeriesStatus.DRAFT);
        when(seriesRepository.findBySeriesIdAndIsDeletedFalse("series-1")).thenReturn(Optional.of(series));
        assertThrows(ContentModuleException.class, () -> seriesService.findPublicEntity("series-1"));
    }
    
    @Test
    void toResponse_Success() {
        series.setStatus(SeriesStatus.PUBLISHED);
        when(seriesCategoryRepository.findBySeries_SeriesIdInAndIsDeletedFalse(any())).thenReturn(List.of());
        when(seriesTagRepository.findBySeries_SeriesIdInAndIsDeletedFalse(any())).thenReturn(List.of());
        SeriesResponseDto response = seriesService.toResponse(series);
        assertNotNull(response);
        assertEquals(series.getSeriesId(), response.getSeriesId());
    }
}
