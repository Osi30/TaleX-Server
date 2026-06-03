package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.SeriesRequestDto;
import com.talex.server.dtos.responses.SeriesResponseDto;
import com.talex.server.entities.Category;
import com.talex.server.entities.Series;
import com.talex.server.entities.SeriesCategory;
import com.talex.server.entities.SeriesTag;
import com.talex.server.entities.Tag;
import com.talex.server.enums.SeriesStatus;
import com.talex.server.enums.Visibility;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.SeriesCategoryRepository;
import com.talex.server.repositories.SeriesRepository;
import com.talex.server.repositories.SeriesTagRepository;
import com.talex.server.services.CategoryService;
import com.talex.server.services.SeriesService;
import com.talex.server.services.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SeriesServiceImpl implements SeriesService {
    private final SeriesRepository seriesRepository;
    private final SeriesCategoryRepository seriesCategoryRepository;
    private final SeriesTagRepository seriesTagRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    @Transactional
    @Override
    public SeriesResponseDto create(SeriesRequestDto request) {
        Series series = new Series();
        applyMutableFields(series, request);
        series.setCreatorId(request.getCreatorId());
        series.markCreatedBy(request.getActorId());

        Series saved = seriesRepository.save(series);
        syncCategories(saved, request.getCategoryIds(), request.getActorId());
        syncTags(saved, request.getTagIds(), request.getActorId());

        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public SeriesResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public SeriesResponseDto getPublicById(String id) {
        return toResponse(findPublicEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> list(Integer page, Integer pageSize) {
        Page<SeriesResponseDto> result = seriesRepository.findAllByIsDeletedFalse(buildPageable(page, pageSize))
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> listByCreator(String creatorId, Integer page, Integer pageSize) {
        if (creatorId == null || creatorId.isBlank()) {
            throw ContentModuleException.badRequest("creatorId is required");
        }

        Page<SeriesResponseDto> result = seriesRepository
                .findAllByCreatorIdAndIsDeletedFalse(creatorId, buildPageable(page, pageSize))
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<SeriesResponseDto> listPublic(Integer page, Integer pageSize) {
        Page<SeriesResponseDto> result = seriesRepository
                .findAllByVisibilityAndStatusAndIsDeletedFalse(
                        Visibility.PUBLIC,
                        SeriesStatus.PUBLISHED,
                        buildPageable(page, pageSize))
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional
    @Override
    public SeriesResponseDto update(String id, SeriesRequestDto request) {
        Series series = findActiveEntity(id);
        applyMutableFields(series, request);
        if (request.getCreatorId() != null) {
            series.setCreatorId(request.getCreatorId());
        }
        series.markUpdatedBy(request.getActorId());

        Series saved = seriesRepository.save(series);
        syncCategories(saved, request.getCategoryIds(), request.getActorId());
        syncTags(saved, request.getTagIds(), request.getActorId());

        return toResponse(saved);
    }

    @Transactional
    @Override
    public SeriesResponseDto publish(String id, String actorId) {
        Series series = findActiveEntity(id);
        series.setStatus(SeriesStatus.PUBLISHED);
        series.setVisibility(Visibility.PUBLIC);
        series.markUpdatedBy(actorId);
        return toResponse(seriesRepository.save(series));
    }

    @Transactional
    @Override
    public SeriesResponseDto hide(String id, String actorId) {
        Series series = findActiveEntity(id);
        series.setStatus(SeriesStatus.HIDDEN);
        series.markUpdatedBy(actorId);
        return toResponse(seriesRepository.save(series));
    }

    @Transactional
    @Override
    public SeriesResponseDto unhide(String id, String actorId) {
        Series series = findActiveEntity(id);
        series.setStatus(SeriesStatus.PUBLISHED);
        series.markUpdatedBy(actorId);
        return toResponse(seriesRepository.save(series));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Series series = findActiveEntity(id);
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
        if (series.getStatus() != SeriesStatus.PUBLISHED || series.getVisibility() != Visibility.PUBLIC) {
            throw ContentModuleException.notFound("Public series not found: " + id);
        }
        return series;
    }

    @Override
    public SeriesResponseDto toResponse(Series series) {
        return SeriesResponseDto.builder()
                .seriesId(series.getSeriesId())
                .creatorId(series.getCreatorId())
                .title(series.getTitle())
                .description(series.getDescription())
                .coverUrl(series.getCoverUrl())
                .bannerUrl(series.getBannerUrl())
                .contentType(series.getContentType())
                .status(series.getStatus())
                .visibility(series.getVisibility())
                .ageRating(series.getAgeRating())
                .language(series.getLanguage())
                .totalViews(series.getTotalViews())
                .totalSubscriptions(series.getTotalSubscriptions())
                .categories(seriesCategoryRepository.findBySeries_SeriesIdAndIsDeletedFalse(series.getSeriesId())
                        .stream()
                        .map(SeriesCategory::getCategory)
                        .map(categoryService::toResponse)
                        .toList())
                .tags(seriesTagRepository.findBySeries_SeriesIdAndIsDeletedFalse(series.getSeriesId())
                        .stream()
                        .map(SeriesTag::getTag)
                        .map(tagService::toResponse)
                        .toList())
                .createdAt(series.getCreatedAt())
                .updatedAt(series.getUpdatedAt())
                .deletedAt(series.getDeletedAt())
                .createdBy(series.getCreatedBy())
                .updatedBy(series.getUpdatedBy())
                .deletedBy(series.getDeletedBy())
                .isDeleted(series.getIsDeleted())
                .build();
    }

    private void applyMutableFields(Series series, SeriesRequestDto request) {
        series.setTitle(request.getTitle());
        series.setDescription(request.getDescription());
        series.setCoverUrl(request.getCoverUrl());
        series.setBannerUrl(request.getBannerUrl());
        series.setContentType(request.getContentType());
        series.setStatus(request.getStatus() != null ? request.getStatus() : series.getStatus());
        series.setVisibility(request.getVisibility() != null ? request.getVisibility() : series.getVisibility());
        series.setAgeRating(request.getAgeRating());
        series.setLanguage(request.getLanguage());
    }

    private void syncCategories(Series series, List<String> categoryIds, String actorId) {
        if (categoryIds == null) {
            return;
        }

        Set<String> requestedIds = cleanIds(categoryIds);
        Map<String, SeriesCategory> existingByCategoryId = seriesCategoryRepository
                .findBySeries_SeriesId(series.getSeriesId())
                .stream()
                .collect(Collectors.toMap(sc -> sc.getCategory().getCategoryId(), Function.identity()));

        for (SeriesCategory relation : existingByCategoryId.values()) {
            String categoryId = relation.getCategory().getCategoryId();
            if (!requestedIds.contains(categoryId) && !Boolean.TRUE.equals(relation.getIsDeleted())) {
                relation.softDelete(actorId);
                seriesCategoryRepository.save(relation);
            }
        }

        for (String categoryId : requestedIds) {
            Category category = categoryService.findAssignableEntity(categoryId);
            SeriesCategory existing = existingByCategoryId.get(categoryId);
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.restore(actorId);
                }
                existing.markUpdatedBy(actorId);
                seriesCategoryRepository.save(existing);
                continue;
            }

            SeriesCategory relation = new SeriesCategory(series, category);
            relation.markCreatedBy(actorId);
            seriesCategoryRepository.save(relation);
        }
    }

    private void syncTags(Series series, List<String> tagIds, String actorId) {
        if (tagIds == null) {
            return;
        }

        Set<String> requestedIds = cleanIds(tagIds);
        Map<String, SeriesTag> existingByTagId = seriesTagRepository
                .findBySeries_SeriesId(series.getSeriesId())
                .stream()
                .collect(Collectors.toMap(st -> st.getTag().getTagId(), Function.identity()));

        for (SeriesTag relation : existingByTagId.values()) {
            String tagId = relation.getTag().getTagId();
            if (!requestedIds.contains(tagId) && !Boolean.TRUE.equals(relation.getIsDeleted())) {
                relation.softDelete(actorId);
                seriesTagRepository.save(relation);
            }
        }

        for (String tagId : requestedIds) {
            Tag tag = tagService.findAssignableEntity(tagId);
            SeriesTag existing = existingByTagId.get(tagId);
            if (existing != null) {
                if (Boolean.TRUE.equals(existing.getIsDeleted())) {
                    existing.restore(actorId);
                }
                existing.markUpdatedBy(actorId);
                seriesTagRepository.save(existing);
                continue;
            }

            SeriesTag relation = new SeriesTag(series, tag);
            relation.markCreatedBy(actorId);
            seriesTagRepository.save(relation);
        }
    }

    private Set<String> cleanIds(List<String> ids) {
        return ids.stream()
                .filter(id -> id != null && !id.isBlank())
                .collect(Collectors.toCollection(HashSet::new));
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        return PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    private BasePageResponse<SeriesResponseDto> toPageResponse(Page<SeriesResponseDto> page) {
        return BasePageResponse.<SeriesResponseDto>builder()
                .content(page.getContent())
                .pageNumber(page.getNumber() + 1)
                .pageSize(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .isFirst(page.isFirst())
                .isLast(page.isLast())
                .build();
    }
}
