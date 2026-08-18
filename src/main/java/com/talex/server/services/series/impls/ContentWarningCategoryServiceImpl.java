package com.talex.server.services.series.impls;

import com.talex.server.dtos.requests.series.ContentWarningCategoryCreateRequestDto;
import com.talex.server.dtos.requests.series.ContentWarningCategoryUpdateRequestDto;
import com.talex.server.dtos.responses.series.ContentWarningCategoryResponseDto;
import com.talex.server.entities.series.ContentWarningCategory;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.series.ContentWarningCategoryRepository;
import com.talex.server.services.series.ContentWarningCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ContentWarningCategoryServiceImpl implements ContentWarningCategoryService {

    private final ContentWarningCategoryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ContentWarningCategoryResponseDto> listActive() {
        return repository.findAllByIsDeletedFalseAndIsActiveTrueOrderByLabelAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ContentWarningCategoryResponseDto> listAll() {
        return repository.findAllByIsDeletedFalseOrderByLabelAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ContentWarningCategoryResponseDto create(UUID accountId, ContentWarningCategoryCreateRequestDto request) {
        String code = request.getCode().trim();
        if (repository.existsByCodeAndIsDeletedFalse(code)) {
            throw ContentModuleException.conflict("Mã nhóm này đã tồn tại: " + code);
        }

        ContentWarningCategory entity = ContentWarningCategory.builder()
                .code(code)
                .label(request.getLabel().trim())
                .isActive(true)
                .build();
        entity.markCreatedBy(accountId.toString());

        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ContentWarningCategoryResponseDto update(UUID accountId, UUID categoryId, ContentWarningCategoryUpdateRequestDto request) {
        ContentWarningCategory entity = findManageableEntity(categoryId);

        entity.setLabel(request.getLabel().trim());
        entity.setIsActive(request.getIsActive());
        entity.markUpdatedBy(accountId.toString());

        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID accountId, UUID categoryId) {
        ContentWarningCategory entity = findManageableEntity(categoryId);
        // Soft-delete — KHÔNG dọn code khỏi series_content_warnings của các series đã khai
        // báo trước đó (giữ lịch sử), chỉ ẩn khỏi danh sách chọn mới (listActive() lọc
        // isDeleted=false). Khớp cách ViolationLabelCategory xử lý xóa.
        entity.softDelete(accountId.toString());
        repository.save(entity);
    }

    private ContentWarningCategory findManageableEntity(UUID categoryId) {
        return repository.findByCategoryIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> ContentModuleException.notFound("Không tìm thấy nhóm: " + categoryId));
    }

    private ContentWarningCategoryResponseDto toResponse(ContentWarningCategory entity) {
        return ContentWarningCategoryResponseDto.builder()
                .categoryId(entity.getCategoryId())
                .code(entity.getCode())
                .label(entity.getLabel())
                .isActive(entity.getIsActive())
                .build();
    }
}
