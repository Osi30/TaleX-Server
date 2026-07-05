package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.TagRequestDto;
import com.talex.server.dtos.responses.TagResponseDto;
import com.talex.server.entities.series.Tag;
import com.talex.server.enums.series.TagStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.TagRepository;
import com.talex.server.services.TagService;
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
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;
    private final ContentAuditLogger contentAuditLogger;

    @Transactional
    @Override
    public TagResponseDto create(TagRequestDto request) {
        String slug = SlugUtils.normalizeSlug(request.getSlug(), request.getTagName());
        ensureSlugAvailable(slug, null);

        Tag tag = new Tag();
        tag.setTagName(request.getTagName());
        tag.setDescription(request.getDescription());
        tag.setSlug(slug);
        tag.setStatus(request.getStatus() != null ? request.getStatus() : TagStatus.ACTIVE);

        Tag saved = tagRepository.save(tag);
        contentAuditLogger.logAction("Tag", saved.getTagId(), "CREATE", request.getActorId(), "");
        return toResponse(saved);
    }

    @Transactional(readOnly = true)
    @Override
    public TagResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<TagResponseDto> list(Integer page, Integer pageSize) {
        Pageable pageable = PageUtils.buildPageable(page, pageSize);
        Page<TagResponseDto> result = tagRepository.findAllByIsDeletedFalse(pageable)
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<TagResponseDto> listPublic(Integer page, Integer pageSize) {
        Pageable pageable = PageUtils.buildPageable(page, pageSize);
        Page<TagResponseDto> result = tagRepository
                .findAllByStatusAndIsDeletedFalse(TagStatus.ACTIVE, pageable)
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional
    @Override
    public TagResponseDto update(String id, TagRequestDto request) {
        Tag tag = findActiveEntity(id);
        String slug = SlugUtils.normalizeSlug(request.getSlug(), request.getTagName());
        ensureSlugAvailable(slug, id);

        tag.setTagName(request.getTagName());
        tag.setDescription(request.getDescription());
        tag.setSlug(slug);
        if (request.getStatus() != null) {
            tag.setStatus(request.getStatus());
        }

        Tag saved = tagRepository.save(tag);
        contentAuditLogger.logAction("Tag", saved.getTagId(), "UPDATE", request.getActorId(), "");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public TagResponseDto hide(String id, String actorId) {
        Tag tag = findActiveEntity(id);
        tag.setStatus(TagStatus.INACTIVE);
        Tag saved = tagRepository.save(tag);
        contentAuditLogger.logAction("Tag", saved.getTagId(), "HIDE", actorId, "");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public TagResponseDto unhide(String id, String actorId) {
        Tag tag = findActiveEntity(id);
        tag.setStatus(TagStatus.ACTIVE);
        Tag saved = tagRepository.save(tag);
        contentAuditLogger.logAction("Tag", saved.getTagId(), "UNHIDE", actorId, "");
        return toResponse(saved);
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Tag tag = findActiveEntity(id);
        tag.setStatus(TagStatus.DELETED);
        tag.softDelete();
        tagRepository.save(tag);
        contentAuditLogger.logAction("Tag", tag.getTagId(), "DELETE", actorId, "");
    }

    @Override
    public Tag findActiveEntity(String id) {
        return tagRepository.findByTagIdAndIsDeletedFalse(id)
                .orElseThrow(() -> ContentModuleException.notFound("Tag not found: " + id));
    }

    @Override
    public Tag findAssignableEntity(String id) {
        Tag tag = findActiveEntity(id);
        if (tag.getStatus() != TagStatus.ACTIVE) {
            throw ContentModuleException.badRequest("Tag is not active: " + id);
        }
        return tag;
    }

    @Override
    public TagResponseDto toResponse(Tag tag) {
        return TagResponseDto.builder()
                .tagId(tag.getTagId())
                .tagName(tag.getTagName())
                .description(tag.getDescription())
                .slug(tag.getSlug())
                .status(tag.getStatus())
                .createdAt(tag.getCreatedAt())
                .updatedAt(tag.getUpdatedAt())
                .deletedAt(tag.getDeletedAt())
                .isDeleted(tag.getIsDeleted())
                .build();
    }

    private void ensureSlugAvailable(String slug, String currentId) {
        tagRepository.findBySlugAndIsDeletedFalse(slug)
                .filter(existing -> !existing.getTagId().equals(currentId))
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Tag slug already exists: " + slug);
                });
    }

    private BasePageResponse<TagResponseDto> toPageResponse(Page<TagResponseDto> page) {
        return BasePageResponse.<TagResponseDto>builder()
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
