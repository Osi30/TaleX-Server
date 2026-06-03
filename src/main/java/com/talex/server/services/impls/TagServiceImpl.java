package com.talex.server.services.impls;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.TagRequestDto;
import com.talex.server.dtos.responses.TagResponseDto;
import com.talex.server.entities.Tag;
import com.talex.server.enums.TagStatus;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.TagRepository;
import com.talex.server.services.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.text.Normalizer;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class TagServiceImpl implements TagService {
    private final TagRepository tagRepository;

    @Transactional
    @Override
    public TagResponseDto create(TagRequestDto request) {
        String slug = normalizeSlug(request.getSlug(), request.getTagName());
        ensureSlugAvailable(slug, null);

        Tag tag = new Tag();
        tag.setTagName(request.getTagName());
        tag.setDescription(request.getDescription());
        tag.setSlug(slug);
        tag.setStatus(request.getStatus() != null ? request.getStatus() : TagStatus.ACTIVE);
        tag.markCreatedBy(request.getActorId());

        return toResponse(tagRepository.save(tag));
    }

    @Transactional(readOnly = true)
    @Override
    public TagResponseDto getById(String id) {
        return toResponse(findActiveEntity(id));
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<TagResponseDto> list(Integer page, Integer pageSize) {
        Pageable pageable = buildPageable(page, pageSize);
        Page<TagResponseDto> result = tagRepository.findAllByIsDeletedFalse(pageable)
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional(readOnly = true)
    @Override
    public BasePageResponse<TagResponseDto> listPublic(Integer page, Integer pageSize) {
        Pageable pageable = buildPageable(page, pageSize);
        Page<TagResponseDto> result = tagRepository
                .findAllByStatusAndIsDeletedFalse(TagStatus.ACTIVE, pageable)
                .map(this::toResponse);
        return toPageResponse(result);
    }

    @Transactional
    @Override
    public TagResponseDto update(String id, TagRequestDto request) {
        Tag tag = findActiveEntity(id);
        String slug = normalizeSlug(request.getSlug(), request.getTagName());
        ensureSlugAvailable(slug, id);

        tag.setTagName(request.getTagName());
        tag.setDescription(request.getDescription());
        tag.setSlug(slug);
        if (request.getStatus() != null) {
            tag.setStatus(request.getStatus());
        }
        tag.markUpdatedBy(request.getActorId());

        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    @Override
    public TagResponseDto hide(String id, String actorId) {
        Tag tag = findActiveEntity(id);
        tag.setStatus(TagStatus.INACTIVE);
        tag.markUpdatedBy(actorId);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    @Override
    public TagResponseDto unhide(String id, String actorId) {
        Tag tag = findActiveEntity(id);
        tag.setStatus(TagStatus.ACTIVE);
        tag.markUpdatedBy(actorId);
        return toResponse(tagRepository.save(tag));
    }

    @Transactional
    @Override
    public void delete(String id, String actorId) {
        Tag tag = findActiveEntity(id);
        tag.setStatus(TagStatus.DELETED);
        tag.softDelete(actorId);
        tagRepository.save(tag);
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
                .createdBy(tag.getCreatedBy())
                .updatedBy(tag.getUpdatedBy())
                .deletedBy(tag.getDeletedBy())
                .isDeleted(tag.getIsDeleted())
                .build();
    }

    private void ensureSlugAvailable(String slug, String currentId) {
        tagRepository.findBySlugAndIsDeletedFalse(slug)
                .filter(existing -> currentId == null || !existing.getTagId().equals(currentId))
                .ifPresent(existing -> {
                    throw ContentModuleException.conflict("Tag slug already exists: " + slug);
                });
    }

    private Pageable buildPageable(Integer page, Integer pageSize) {
        int safePage = page == null || page < 1 ? 1 : page;
        int safePageSize = pageSize == null || pageSize < 1 ? 20 : pageSize;
        return PageRequest.of(safePage - 1, safePageSize, Sort.by(Sort.Direction.DESC, "createdAt"));
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

    private String normalizeSlug(String slug, String fallback) {
        String value = slug == null || slug.isBlank() ? fallback : slug;
        String normalized = Normalizer.normalize(value, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "-")
                .replaceAll("(^-|-$)", "");

        if (normalized.isBlank()) {
            throw ContentModuleException.badRequest("Slug cannot be empty");
        }
        return normalized;
    }
}
