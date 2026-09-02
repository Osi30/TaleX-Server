package com.talex.server.services.series.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.analytic.SeriesLogResponseDto;
import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.dtos.requests.series.SeriesRequestDto;
import com.talex.server.dtos.requests.series.SeriesSearchCriteria;
import com.talex.server.dtos.responses.series.SeriesResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.series.*;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.series.CategoryStatus;
import com.talex.server.enums.series.SeasonStatus;
import com.talex.server.enums.series.SeriesStatus;
import com.talex.server.enums.series.TagStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.mappers.series.SeriesMapper;
import com.talex.server.repositories.campaign.CampaignSeriesRepository;
import com.talex.server.repositories.series.*;
import com.talex.server.services.audit.ContentAuditLogger;
import com.talex.server.services.creator.CreatorService;
import com.talex.server.services.media.impls.ContentOwnershipService;
import com.talex.server.services.mongo.SeriesFeatureService;
import com.talex.server.services.series.SeriesService;
import com.talex.server.specifications.SeriesSpec;
import com.talex.server.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {
    private final SeriesRepository seriesRepository;
    private final SeriesCategoryRepository seriesCategoryRepository;
    private final SeriesTagRepository seriesTagRepository;
    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;
    private final CampaignSeriesRepository campaignSeriesRepository;
    private final SeriesLogRepository seriesLogRepository;
    private final ContentOwnershipService contentOwnershipService;
    private final SeriesFeatureService seriesFeatureService;
    private final CreatorService creatorService;
    private final SeasonRepository seasonRepository;
    private final ContentAuditLogger contentAuditLogger;
    private final ContentCascadeDeleteHelper contentCascadeDeleteHelper;
    private final SeriesMapper seriesMapper;
    private final ContentWarningCategoryRepository contentWarningCategoryRepository;

    @Transactional
    @Override
    public SeriesResponseDto create(SeriesRequestDto request, UUID accountId) {
        Creator creator = creatorService.getEntityByAccountId(accountId);
        String accountIdStr = accountId.toString();

        Series series = new Series();
        applyMutableFields(series, request);
        series.setStatus(SeriesStatus.DRAFT);
        series.setReleasedUpdateTime(LocalDateTime.now());
        series.setCreator(creator);

        Series saved = seriesRepository.save(series);
        Map<String, Category> assignedCategories = syncCategories(saved, request.getCategoryIds(), accountIdStr);
        Map<String, Tag> assignedTags = syncTags(saved, request.getTagIds(), accountIdStr);

        seriesFeatureService.saveSeriesMetadata(series, assignedCategories, assignedTags);
        createDefaultSeason(saved, creator, accountIdStr);
        
        contentAuditLogger.logAction("Series", saved.getSeriesId(), "CREATE", accountIdStr, creator.getCreatorId());

        return seriesMapper.toResponse(saved);
    }

    private void createDefaultSeason(Series series, Creator creator, String accountIdStr) {
        Season season = new Season();
        season.setSeries(series);
        season.setCreatorId(creator.getCreatorId());
        season.setSeasonNumber(1);
        season.setTitle("Season 1");
        season.setDescription("Phần đầu tiên của series");
        season.setStatus(com.talex.server.enums.series.SeasonStatus.DRAFT);
        
        seasonRepository.save(season);
        contentAuditLogger.logAction("Season", season.getSeasonId(), "CREATE", accountIdStr, creator.getCreatorId());
    }

    @Transactional(readOnly = true)
    @Override
    public SeriesResponseDto getById(String id, String accountId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanView(series, accountId);
        return seriesMapper.toResponse(series);
    }

    @Transactional(readOnly = true)
    @Override
    public SeriesResponseDto getPublicById(String id) {
        Series series = seriesRepository.findActiveSeriesWithAvatarById(id)
                .orElseThrow(() -> ContentModuleException.notFound("Public series not found: " + id));
        if (series.getStatus() != SeriesStatus.PUBLISHED && series.getStatus() != SeriesStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public series not found: " + id);
        }
        return seriesMapper.toResponse(series);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> list(Integer page, Integer pageSize) {
        Page<Series> result = seriesRepository.findAllByIsDeletedFalse(PageUtils.buildPageable(page, pageSize));
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> listByCreator(
            UUID accountId,
            List<SeriesStatus> statuses,
            Integer page,
            Integer pageSize
    ) {
        String creatorId = creatorService.getEntityByAccountId(accountId).getCreatorId();
        Page<Series> result;
        if (statuses != null && !statuses.isEmpty()) {
            result = seriesRepository.findAllByCreator_CreatorIdAndStatusInAndIsDeletedFalse(
                    creatorId,
                    statuses,
                    PageUtils.buildPageable(page, pageSize)
            );
        } else {
            result = seriesRepository.findAllByCreator_CreatorIdAndIsDeletedFalse(
                    creatorId,
                    PageUtils.buildPageable(page, pageSize)
            );
        }
        return toPageResponse(result);
    }

    @Override
    public BasePageResponse<SeriesResponseDto> listByCreatorAndCampaign(UUID accountId, List<SeriesStatus> statuses, Integer page, Integer pageSize) {
        String creatorId = creatorService.getEntityByAccountId(accountId).getCreatorId();

        // Định nghĩa các trạng thái CampaignSeries được coi là đang hoạt động[cite: 18]
        List<CampaignStatus> activeCampaignStatuses = List.of(CampaignStatus.RUNNING, CampaignStatus.PAUSED);

        Page<Series> result;
        if (statuses != null && !statuses.isEmpty()) {
            result = seriesRepository.findAllByCreatorAndStatusInAndNotInActiveCampaign(
                    creatorId,
                    statuses,
                    activeCampaignStatuses,
                    PageUtils.buildPageable(page, pageSize)
            );
        } else {
            result = seriesRepository.findAllByCreatorAndNotInActiveCampaign(
                    creatorId,
                    activeCampaignStatuses,
                    PageUtils.buildPageable(page, pageSize)
            );
        }
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> listPublic(Integer page, Integer pageSize) {
        Page<Series> result = seriesRepository
                .findPublicSeriesWithAvatar(
                        List.of(SeriesStatus.PUBLISHED, SeriesStatus.SCHEDULED),
                        PageUtils.buildPageable(page, pageSize));
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public Slice<SeriesCardResponseDto> searchPublicSeries(SeriesSearchCriteria criteria, Pageable pageable) {
        // 1. Tạo Specification từ criteria
        Specification<Series> spec = SeriesSpec.filterByCriteria(criteria);

        // 2. Tự động xử lý Sort theo thuộc tính entity (chuyển đổi alias nếu cần)
        Pageable sortedPageable = buildSortedPageable(criteria, pageable);

        // 3. Gọi repository - Spring Data JPA tự phân trang Slice
        Slice<Series> seriesSlice = seriesRepository.findBy(spec, q -> q
                .slice(sortedPageable)
        );

        // 4. Biến đổi Entity -> DTO bằng hàm .map() có sẵn của Slice
        assert seriesSlice != null;
        return seriesSlice.map(seriesMapper::toCardDto);
    }

    @Transactional(readOnly = true)
    @Override
    public List<SeriesCardResponseDto> getSeriesCardsByIds(List<String> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Lọc trùng ID (Deduplicate) và loại bỏ null/blank nhưng vẫn giữ nguyên thứ tự ban đầu
        List<String> distinctIds = seriesIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Query danh sách Series từ DB
        List<Series> seriesList = seriesRepository.findAllBySeriesIdInAndStatus(distinctIds, SeriesStatus.PUBLISHED);

        // 3. Map danh sách Series theo seriesId để dễ tra cứu
        Map<String, Series> seriesMap = seriesList.stream()
                .collect(Collectors.toMap(Series::getSeriesId, Function.identity(), (s1, s2) -> s1));

        // 4. Duyệt lại theo danh sách distinctIds ban đầu để bảo toàn đúng thứ tự thứ hạng/khuyên dùng
        return distinctIds.stream()
                .map(seriesMap::get)
                .filter(Objects::nonNull)
                .map(seriesMapper::toCardDto)
                .toList();
    }

    @Override
    public List<SeriesCardResponseDto> getPromotedSeriesCardsByIds(List<String> seriesIds) {
        if (seriesIds == null || seriesIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 1. Lọc trùng ID (Deduplicate) và loại bỏ null/blank nhưng vẫn giữ nguyên thứ tự ban đầu
        List<String> distinctIds = seriesIds.stream()
                .filter(id -> id != null && !id.isBlank())
                .distinct()
                .toList();

        if (distinctIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Lọc danh sách seriesIds thông qua CampaignSeries (chỉ giữ các series thuộc campaign có status RUNNING)
        Set<String> runningSeriesIds = new HashSet<>(
                campaignSeriesRepository.findSeriesIdsBySeriesIdInAndStatus(distinctIds, CampaignStatus.RUNNING)
        );

        List<String> validIds = distinctIds.stream()
                .filter(runningSeriesIds::contains)
                .toList();

        if (validIds.isEmpty()) {
            return Collections.emptyList();
        }

        // 2. Query danh sách Series từ DB
        List<Series> seriesList = seriesRepository.findAllBySeriesIdInAndStatus(validIds, SeriesStatus.PUBLISHED);

        // 3. Map danh sách Series theo seriesId để dễ tra cứu
        Map<String, Series> seriesMap = seriesList.stream()
                .collect(Collectors.toMap(Series::getSeriesId, Function.identity(), (s1, s2) -> s1));

        // 4. Duyệt lại theo danh sách distinctIds ban đầu để bảo toàn đúng thứ tự thứ hạng/khuyên dùng
        return distinctIds.stream()
                .map(seriesMap::get)
                .filter(Objects::nonNull)
                .map(seriesMapper::toCardDto)
                .toList();
    }

    private Pageable buildSortedPageable(SeriesSearchCriteria criteria, Pageable pageable) {
        String property = switch (Optional.ofNullable(criteria.getSortBy()).orElse("").toLowerCase()) {
            case "averagerating" -> "averageRating";
            case "releasedupdatetime" -> "releasedUpdateTime";
            case "likes" -> "analyticData.likes";
            case "views" -> "analyticData.views";
            case "watchtime" -> "analyticData.watchTime";
            default -> "releasedUpdateTime";
        };

        Sort.Direction direction = "ASC".equalsIgnoreCase(criteria.getSortDirection())
                ? Sort.Direction.ASC
                : Sort.Direction.DESC;

        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), Sort.by(direction, property));
    }

    @Transactional
    @Override
    public SeriesResponseDto update(String id, SeriesRequestDto request, String accountId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, accountId);
        if (request.getStatus() == SeriesStatus.SCHEDULED) {
            throw ContentModuleException.badRequest("SCHEDULED is managed by episode publish scheduling");
        }
        applyMutableFields(series, request);

        Series saved = seriesRepository.save(series);
        Map<String, Category> assignedCategories = syncCategories(saved, request.getCategoryIds(), accountId);
        Map<String, Tag> assignedTags = syncTags(saved, request.getTagIds(), accountId);

        // Update SeriesMetadata in MongoDB
        seriesFeatureService.saveSeriesMetadata(series, assignedCategories, assignedTags);

        contentAuditLogger.logAction("Series", saved.getSeriesId(), "UPDATE", accountId, series.getCreator().getCreatorId());

        return seriesMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public SeriesResponseDto hide(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.HIDDEN);
        series.setReleasedUpdateTime(java.time.LocalDateTime.now());
        Series saved = seriesRepository.save(series);
        contentAuditLogger.logAction("Series", saved.getSeriesId(), "HIDE", actorId, series.getCreator().getCreatorId());
        return seriesMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public SeriesResponseDto unhide(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.PUBLISHED);
        series.setReleasedUpdateTime(java.time.LocalDateTime.now());
        Series saved = seriesRepository.save(series);
        contentAuditLogger.logAction("Series", saved.getSeriesId(), "UNHIDE", actorId, series.getCreator().getCreatorId());
        return seriesMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public SeriesResponseDto forceHide(String id, String actorId) {
        Series series = findActiveEntity(id);
        series.setStatus(SeriesStatus.FORCE_HIDDEN);
        series.setReleasedUpdateTime(java.time.LocalDateTime.now());
        Series saved = seriesRepository.save(series);
        contentAuditLogger.logAction("Series", saved.getSeriesId(), "FORCE_HIDE", actorId, series.getCreator().getCreatorId());
        return seriesMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public SeriesResponseDto forceUnhide(String id, String actorId) {
        Series series = findActiveEntity(id);
        if (series.getStatus() != SeriesStatus.FORCE_HIDDEN) {
            throw ContentModuleException.badRequest("Series is not force-hidden");
        }
        series.setStatus(SeriesStatus.HIDDEN);
        series.setReleasedUpdateTime(java.time.LocalDateTime.now());
        Series saved = seriesRepository.save(series);
        contentAuditLogger.logAction("Series", saved.getSeriesId(), "FORCE_UNHIDE", actorId, series.getCreator().getCreatorId());
        return seriesMapper.toResponse(saved);
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.DELETED);
        series.setReleasedUpdateTime(java.time.LocalDateTime.now());
        series.softDelete();
        seriesRepository.save(series);
        cascadeDeleteSeasons(series.getSeriesId(), actorId);
        contentAuditLogger.logAction("Series", series.getSeriesId(), "DELETE", actorId, series.getCreator().getCreatorId());
    }

    private void cascadeDeleteSeasons(String seriesId, String actorId) {
        List<Season> seasons = seasonRepository
                .findAllBySeries_SeriesIdAndIsDeletedFalseOrderBySeasonNumberAsc(seriesId);
        for (Season season : seasons) {
            season.setStatus(SeasonStatus.DELETED);
            season.setReleasedUpdateTime(java.time.LocalDateTime.now());
            season.softDelete();
            contentCascadeDeleteHelper.cascadeDeleteSeasonEpisodes(season.getSeasonId(), actorId);
        }
        seasonRepository.saveAll(seasons);
    }

    @Override
    public Series findActiveEntity(String id) {
        return seriesRepository.findBySeriesIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Series not found: " + id));
    }

    @Override
    public Series findPublicEntity(String id) {
        Series series = findActiveEntity(id);
        if (series.getStatus() != SeriesStatus.PUBLISHED && series.getStatus() != SeriesStatus.SCHEDULED) {
            throw ContentModuleException.notFound("Public series not found: " + id);
        }
        return series;
    }

    @Override
    public List<SeriesLogResponseDto> getSeriesLogs(String id, LocalDateTime start, LocalDateTime end, String accountId) {
        // 2. Query log từ database
        List<SeriesLog> logs = seriesLogRepository.findBySeriesSeriesIdAndHourBucketBetweenOrderByHourBucketAsc(id, start, end);

        // 3. Map sang DTO
        return logs.stream()
                .map(log -> SeriesLogResponseDto.builder()
                        .seriesLogId(log.getSeriesLogId())
                        .hourBucket(log.getHourBucket())
                        .seriesId(log.getSeries().getSeriesId())
                        .analyticData(log.getAnalyticData())
                        .build())
                .toList();
    }

    private void applyMutableFields(Series series, SeriesRequestDto request) {
        series.setTitle(request.getTitle());
        series.setDescription(request.getDescription());
        series.setCoverUrl(request.getCoverUrl());
        series.setBannerUrl(request.getBannerUrl());
        series.setContentType(request.getContentType());
        if (request.getStatus() != null && series.getStatus() != request.getStatus()) {
            series.setStatus(request.getStatus());
            series.setReleasedUpdateTime(java.time.LocalDateTime.now());
        }
        series.setAgeRating(request.getAgeRating());
        // null = FE không gửi field này (client cũ) — giữ nguyên giá trị hiện có, giống
        // pattern null-check của categoryIds/tagIds ở syncCategories/syncTags bên dưới.
        if (request.getContentWarnings() != null) {
            series.setContentWarnings(validateContentWarningCodes(request.getContentWarnings()));
        }
        series.setLanguage(request.getLanguage());
    }

    // Chặn code rác/gõ sai — CHỈ cần code tồn tại trong bảng ContentWarningCategory
    // (isDeleted=false), KHÔNG bắt buộc isActive=true. Nếu chặn cả inactive, series đã khai
    // 1 nhóm trước đó rồi Admin ẩn nhóm này đi sẽ bị silently mất luôn khai báo cũ mỗi lần
    // Creator lưu lại series (kể cả khi họ chỉ sửa field khác, không liên quan content
    // warning) — không mong muốn, giữ nguyên khai báo cũ, chỉ ẩn nhóm khỏi form chọn MỚI.
    private Set<String> validateContentWarningCodes(Set<String> codes) {
        if (codes.isEmpty()) {
            return codes;
        }
        List<String> validCodes = contentWarningCategoryRepository
                .findAllByCodeInAndIsDeletedFalse(new ArrayList<>(codes))
                .stream()
                .map(ContentWarningCategory::getCode)
                .toList();
        Set<String> invalid = new HashSet<>(codes);
        invalid.removeAll(validCodes);
        if (!invalid.isEmpty()) {
            throw ContentModuleException.badRequest("Nhóm cảnh báo nội dung không hợp lệ: " + invalid);
        }
        return codes;
    }

    private Map<String, Category> syncCategories(Series series, List<String> categoryIds, String actorId) {
        if (categoryIds == null) {
            return Map.of();
        }

        Set<String> requestedIds = cleanIds(categoryIds);
        Map<String, Category> assignableCategories = loadAssignableCategories(requestedIds);
        Map<String, SeriesCategory> existingByCategoryId = seriesCategoryRepository
                .findBySeries_SeriesId(series.getSeriesId())
                .stream()
                .collect(Collectors.toMap(sc -> sc.getId().getCategoryId(), Function.identity()));
        List<SeriesCategory> changedRelations = new ArrayList<>();

        for (SeriesCategory relation : existingByCategoryId.values()) {
            String categoryId = relation.getId().getCategoryId();
            if (!requestedIds.contains(categoryId) && !Boolean.TRUE.equals(relation.getIsDeleted())) {
                relation.softDelete();
                changedRelations.add(relation);
            }
        }

        for (String categoryId : requestedIds) {
            Category category = assignableCategories.get(categoryId);
            SeriesCategory existing = existingByCategoryId.get(categoryId);
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.restore();
                    changedRelations.add(existing);
                }
                continue;
            }

            SeriesCategory relation = new SeriesCategory(series, category);

            changedRelations.add(relation);
        }

        if (!changedRelations.isEmpty()) {
            seriesCategoryRepository.saveAll(changedRelations);
        }
        
        return assignableCategories;
    }

    private Map<String, Tag> syncTags(Series series, List<String> tagIds, String actorId) {
        if (tagIds == null) {
            return Map.of();
        }

        Set<String> requestedIds = cleanIds(tagIds);
        Map<String, Tag> assignableTags = loadAssignableTags(requestedIds);
        Map<String, SeriesTag> existingByTagId = seriesTagRepository
                .findBySeries_SeriesId(series.getSeriesId())
                .stream()
                .collect(Collectors.toMap(st -> st.getId().getTagId(), Function.identity()));
        List<SeriesTag> changedRelations = new ArrayList<>();

        for (SeriesTag relation : existingByTagId.values()) {
            String tagId = relation.getId().getTagId();
            if (!requestedIds.contains(tagId) && !Boolean.TRUE.equals(relation.getIsDeleted())) {
                relation.softDelete();
                changedRelations.add(relation);
            }
        }

        for (String tagId : requestedIds) {
            Tag tag = assignableTags.get(tagId);
            SeriesTag existing = existingByTagId.get(tagId);
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.restore();
                    changedRelations.add(existing);
                }
                continue;
            }

            SeriesTag relation = new SeriesTag(series, tag);

            changedRelations.add(relation);
        }

        if (!changedRelations.isEmpty()) {
            seriesTagRepository.saveAll(changedRelations);
        }
        
        return assignableTags;
    }

    private Set<String> cleanIds(List<String> ids) {
        return ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private Map<String, Category> loadAssignableCategories(Set<String> requestedIds) {
        if (requestedIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Category> categoriesById = categoryRepository.findAllByCategoryIdInAndIsDeletedFalse(requestedIds)
                .stream()
                .collect(Collectors.toMap(Category::getCategoryId, Function.identity()));
        for (String categoryId : requestedIds) {
            Category category = categoriesById.get(categoryId);
            if (category == null) {
                throw ContentModuleException.notFound("Category not found: " + categoryId);
            }
            if (category.getStatus() != CategoryStatus.ACTIVE) {
                throw ContentModuleException.badRequest("Category is not active: " + categoryId);
            }
        }
        return categoriesById;
    }

    private Map<String, Tag> loadAssignableTags(Set<String> requestedIds) {
        if (requestedIds.isEmpty()) {
            return Map.of();
        }

        Map<String, Tag> tagsById = tagRepository.findAllByTagIdInAndIsDeletedFalse(requestedIds)
                .stream()
                .collect(Collectors.toMap(Tag::getTagId, Function.identity()));
        for (String tagId : requestedIds) {
            Tag tag = tagsById.get(tagId);
            if (tag == null) {
                throw ContentModuleException.notFound("Tag not found: " + tagId);
            }
            if (tag.getStatus() != TagStatus.ACTIVE) {
                throw ContentModuleException.badRequest("Tag is not active: " + tagId);
            }
        }
        return tagsById;
    }

    private BasePageResponse<SeriesResponseDto> toPageResponse(Page<Series> page) {
        List<SeriesResponseDto> content = page.getContent()
                .stream().map(seriesMapper::toResponse)
                .toList();

        return BasePageResponse.<SeriesResponseDto>builder()
                .content(content)
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
