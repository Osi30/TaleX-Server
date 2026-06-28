package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.SeriesRequestDto;
import com.talex.server.dtos.responses.SeriesResponseDto;
import com.talex.server.entities.creator.Creator;
import com.talex.server.entities.series.*;
import com.talex.server.enums.CategoryStatus;
import com.talex.server.enums.SeriesStatus;
import com.talex.server.enums.TagStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.*;
import com.talex.server.services.CategoryService;
import com.talex.server.services.ContentOwnershipService;
import com.talex.server.services.SeriesService;
import com.talex.server.services.TagService;
import com.talex.server.services.creator.ICreatorService;
import com.talex.server.utils.PageUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
    private final CategoryService categoryService;
    private final TagService tagService;
    private final ContentOwnershipService contentOwnershipService;
    private final ICreatorService creatorService;

    @Transactional
    @Override
    public SeriesResponseDto create(SeriesRequestDto request, UUID accountId) {
        Creator creator = creatorService.getEntityByAccountId(accountId);
        String accountIdStr = accountId.toString();

        Series series = new Series();
        applyMutableFields(series, request);
        series.setStatus(SeriesStatus.DRAFT);
        series.setCreator(creator);
        series.markCreatedBy(accountIdStr);

        Series saved = seriesRepository.save(series);
        syncCategories(saved, request.getCategoryIds(), accountIdStr);
        syncTags(saved, request.getTagIds(), accountIdStr);

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public SeriesResponseDto getById(String id, String accountId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, accountId);
        return toResponse(series);
    }

    @Transactional(readOnly = true)
    @Override
    public SeriesResponseDto getPublicById(String id) {
        return toResponse(findPublicEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> list(Integer page, Integer pageSize) {
        Page<Series> result = seriesRepository.findAllByIsDeletedFalse(PageUtils.buildPageable(page, pageSize));
        return toPageResponse(result, toResponses(result.getContent()));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> listByCreator(UUID accountId, Integer page, Integer pageSize) {
        String creatorId = creatorService.getEntityByAccountId(accountId).getCreatorId();
        Page<Series> result = seriesRepository
                .findAllByCreator_CreatorIdAndIsDeletedFalse(creatorId, PageUtils.buildPageable(page, pageSize));
        return toPageResponse(result, toResponses(result.getContent()));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> listPublic(Integer page, Integer pageSize) {
        Page<Series> result = seriesRepository
                .findAllByStatusAndIsDeletedFalse(
                        SeriesStatus.PUBLISHED,
                        PageUtils.buildPageable(page, pageSize));
        return toPageResponse(result, toResponses(result.getContent()));
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
        series.markUpdatedBy(accountId);

        Series saved = seriesRepository.save(series);
        syncCategories(saved, request.getCategoryIds(), accountId);
        syncTags(saved, request.getTagIds(), accountId);

        return toResponse(saved);
    }

    @Transactional
    @Override
    public SeriesResponseDto publish(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.PUBLISHED);
        series.markUpdatedBy(actorId);
        return toResponse(seriesRepository.save(series));
    }

    @Transactional
    @Override
    public SeriesResponseDto hide(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.HIDDEN);
        series.markUpdatedBy(actorId);
        return toResponse(seriesRepository.save(series));
    }

    @Transactional
    @Override
    public SeriesResponseDto unhide(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.PUBLISHED);
        series.markUpdatedBy(actorId);
        return toResponse(seriesRepository.save(series));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Series series = findActiveEntity(id);
        contentOwnershipService.assertCanManage(series, actorId);
        series.setStatus(SeriesStatus.DELETED);
        series.softDelete(actorId);
        seriesRepository.save(series);
    }

    @Override
    public Series findActiveEntity(String id) {
        return seriesRepository.findBySeriesIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Series not found: " + id));
    }

    @Override
    public Series findPublicEntity(String id) {
        Series series = findActiveEntity(id);
        if (series.getStatus() != SeriesStatus.PUBLISHED) {
            throw ContentModuleException.notFound("Public series not found: " + id);
        }
        return series;
    }

    @Override
    public SeriesResponseDto toResponse(Series series) {
        return toResponses(List.of(series)).getFirst();
    }

    private List<SeriesResponseDto> toResponses(List<Series> seriesList) {
        if (seriesList.isEmpty()) {
            return List.of();
        }

        List<String> seriesIds = seriesList.stream()
                .map(Series::getSeriesId)
                .toList();
        Map<String, List<com.talex.server.dtos.responses.CategoryResponseDto>> categoriesBySeriesId =
                loadCategoryResponses(seriesIds);
        Map<String, List<com.talex.server.dtos.responses.TagResponseDto>> tagsBySeriesId =
                loadTagResponses(seriesIds);

        return seriesList.stream()
                .map(series -> toResponse(
                        series,
                        categoriesBySeriesId.getOrDefault(series.getSeriesId(), List.of()),
                        tagsBySeriesId.getOrDefault(series.getSeriesId(), List.of())))
                .toList();
    }

    private SeriesResponseDto toResponse(
            Series series,
            List<com.talex.server.dtos.responses.CategoryResponseDto> categories,
            List<com.talex.server.dtos.responses.TagResponseDto> tags) {
        return SeriesResponseDto.builder()
                .seriesId(series.getSeriesId())
                .creatorId(series.getCreator().getCreatorId())
                .title(series.getTitle())
                .description(series.getDescription())
                .coverUrl(series.getCoverUrl())
                .bannerUrl(series.getBannerUrl())
                .contentType(series.getContentType())
                .status(series.getStatus())
                .ageRating(series.getAgeRating())
                .language(series.getLanguage())
                .totalViews(series.getTotalViews())
                .totalSubscriptions(series.getTotalSubscriptions())
                .categories(categories)
                .tags(tags)
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .deletedAt(series.getDeletedAt())
                .createdBy(series.getCreatedBy())
                .updatedBy(series.getUpdatedBy())
                .deletedBy(series.getDeletedBy())
                .isDeleted(series.getIsDeleted())
                .build();
    }

    private Map<String, List<com.talex.server.dtos.responses.CategoryResponseDto>> loadCategoryResponses(
            Collection<String> seriesIds) {
        return seriesCategoryRepository.findBySeries_SeriesIdInAndIsDeletedFalse(seriesIds)
                .stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getId().getSeriesId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                relation -> categoryService.toResponse(relation.getCategory()),
                                Collectors.toList())));
    }

    private Map<String, List<com.talex.server.dtos.responses.TagResponseDto>> loadTagResponses(
            Collection<String> seriesIds) {
        return seriesTagRepository.findBySeries_SeriesIdInAndIsDeletedFalse(seriesIds)
                .stream()
                .collect(Collectors.groupingBy(
                        relation -> relation.getId().getSeriesId(),
                        LinkedHashMap::new,
                        Collectors.mapping(
                                relation -> tagService.toResponse(relation.getTag()),
                                Collectors.toList())));
    }

    private void applyMutableFields(Series series, SeriesRequestDto request) {
        series.setTitle(request.getTitle());
        series.setDescription(request.getDescription());
        series.setCoverUrl(request.getCoverUrl());
        series.setBannerUrl(request.getBannerUrl());
        series.setContentType(request.getContentType());
        series.setStatus(request.getStatus() != null ? request.getStatus() : series.getStatus());
        series.setAgeRating(request.getAgeRating());
        series.setLanguage(request.getLanguage());
    }

    private void syncCategories(Series series, List<String> categoryIds, String actorId) {
        if (categoryIds == null) {
            return;
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
                relation.softDelete(actorId);
                changedRelations.add(relation);
            }
        }

        for (String categoryId : requestedIds) {
            Category category = assignableCategories.get(categoryId);
            SeriesCategory existing = existingByCategoryId.get(categoryId);
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.restore(actorId);
                    changedRelations.add(existing);
                }
                continue;
            }

            SeriesCategory relation = new SeriesCategory(series, category);
            relation.markCreatedBy(actorId);
            changedRelations.add(relation);
        }

        if (!changedRelations.isEmpty()) {
            seriesCategoryRepository.saveAll(changedRelations);
        }
    }

    private void syncTags(Series series, List<String> tagIds, String actorId) {
        if (tagIds == null) {
            return;
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
                relation.softDelete(actorId);
                changedRelations.add(relation);
            }
        }

        for (String tagId : requestedIds) {
            Tag tag = assignableTags.get(tagId);
            SeriesTag existing = existingByTagId.get(tagId);
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.restore(actorId);
                    changedRelations.add(existing);
                }
                continue;
            }

            SeriesTag relation = new SeriesTag(series, tag);
            relation.markCreatedBy(actorId);
            changedRelations.add(relation);
        }

        if (!changedRelations.isEmpty()) {
            seriesTagRepository.saveAll(changedRelations);
        }
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

    private BasePageResponse<SeriesResponseDto> toPageResponse(Page<Series> page, List<SeriesResponseDto> content) {
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
