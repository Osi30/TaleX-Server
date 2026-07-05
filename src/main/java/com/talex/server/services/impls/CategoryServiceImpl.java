package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.CategoryRequestDto;
import com.talex.server.dtos.responses.CategoryResponseDto;
import com.talex.server.entities.series.Category;
import com.talex.server.enums.series.CategoryStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.CategoryRepository;
import com.talex.server.services.CategoryService;
import com.talex.server.utils.PageUtils;
import com.talex.server.utils.SlugUtils;
import com.talex.server.services.audit.ContentAuditLogger;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class CategoryServiceImpl implements CategoryService {
    private final CategoryRepository categoryRepository;
    private final ContentAuditLogger contentAuditLogger;

    @Transactional
    @Override
    public CategoryResponseDto create(CategoryRequestDto request) {
        String slug = SlugUtils.normalizeSlug(request.getSlug(), request.getCategoryName());
        ensureSlugAvailable(slug, null);

        Category category = new Category();
        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setSlug(slug);
        category.setStatus(request.getStatus() != null ? request.getStatus() : CategoryStatus.ACTIVE);

        Category saved = categoryRepository.save(category);
        contentAuditLogger.logAction("Category", saved.getCategoryId(), "CREATE", request.getActorId(), "");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public CategoryResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<CategoryResponseDto> list(Integer page, Integer pageSize) {
        Pageable pageable = PageUtils.buildPageable(page, pageSize);
        Page<CategoryResponseDto> result = categoryRepository.findAllByIsDeletedFalse(pageable)
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<CategoryResponseDto> listPublic(Integer page, Integer pageSize) {
        Pageable pageable = PageUtils.buildPageable(page, pageSize);
        Page<CategoryResponseDto> result = categoryRepository
                .findAllByStatusAndIsDeletedFalse(CategoryStatus.ACTIVE, pageable)
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional
    @Override
    public CategoryResponseDto update(String id, CategoryRequestDto request) {
        Category category = findActiveEntity(id);
        String slug = SlugUtils.normalizeSlug(request.getSlug(), request.getCategoryName());
        ensureSlugAvailable(slug, id);

        category.setCategoryName(request.getCategoryName());
        category.setDescription(request.getDescription());
        category.setSlug(slug);
        if (request.getStatus() != null) {
            category.setStatus(request.getStatus());
        }

        Category saved = categoryRepository.save(category);
        contentAuditLogger.logAction("Category", saved.getCategoryId(), "UPDATE", request.getActorId(), "");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public CategoryResponseDto hide(String id, String actorId) {
        Category category = findActiveEntity(id);
        category.setStatus(CategoryStatus.INACTIVE);
        Category saved = categoryRepository.save(category);
        contentAuditLogger.logAction("Category", saved.getCategoryId(), "HIDE", actorId, "");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public CategoryResponseDto unhide(String id, String actorId) {
        Category category = findActiveEntity(id);
        category.setStatus(CategoryStatus.ACTIVE);
        Category saved = categoryRepository.save(category);
        contentAuditLogger.logAction("Category", saved.getCategoryId(), "UNHIDE", actorId, "");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Category category = findActiveEntity(id);
        category.setStatus(CategoryStatus.DELETED);
        category.softDelete();
        categoryRepository.save(category);
        contentAuditLogger.logAction("Category", category.getCategoryId(), "DELETE", actorId, "");
    }

    @Override
    public Category findActiveEntity(String id) {
        return categoryRepository.findByCategoryIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Category not found: " + id));
    }

    @Override
    public Category findAssignableEntity(String id) {
        Category category = findActiveEntity(id);
        if (category.getStatus() != CategoryStatus.ACTIVE) {
            throw ContentModuleException.badRequest("Category is not active: " + id);
        }
        return category;
    }

    @Override
    public CategoryResponseDto toResponse(Category category) {
        return CategoryResponseDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .slug(category.getSlug())
                .status(category.getStatus())
                .createdAt(category.getCreatedAt())
                .updatedAt(category.getUpdatedAt())
                .deletedAt(category.getDeletedAt())
                .isDeleted(category.getIsDeleted())
                .build();
    }

    private void ensureSlugAvailable(String slug, String currentId) {
        categoryRepository.findBySlugAndIsDeletedFalse(slug)
                .filter(existing -> !existing.getCategoryId().equals(currentId))
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Category slug already exists: " + slug);
                });
    }

    private BasePageResponse<CategoryResponseDto> toPageResponse(Page<CategoryResponseDto> page) {
        return BasePageResponse.<CategoryResponseDto>builder()
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
