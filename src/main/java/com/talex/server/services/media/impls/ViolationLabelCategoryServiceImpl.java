package com.talex.server.services.media.impls;

import com.talex.server.dtos.requests.media.ViolationLabelCategoryRequestDto;
import com.talex.server.dtos.responses.media.ViolationLabelCategoryResponseDto;
import com.talex.server.entities.media.ViolationLabelCategory;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.repositories.media.ViolationLabelCategoryRepository;
import com.talex.server.services.media.ViolationLabelCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ViolationLabelCategoryServiceImpl implements ViolationLabelCategoryService {

    private final ViolationLabelCategoryRepository repository;

    @Override
    @Transactional(readOnly = true)
    public List<ViolationLabelCategoryResponseDto> list() {
        return repository.findAllByIsDeletedFalseOrderByNameAsc().stream()
                .map(this::toResponse)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public ViolationLabelCategoryResponseDto create(UUID accountId, ViolationLabelCategoryRequestDto request) {
        String name = request.getName().trim();
        if (repository.existsByNameAndIsDeletedFalse(name)) {
            throw ContentModuleException.conflict("Nhóm này đã tồn tại: " + name);
        }

        ViolationLabelCategory entity = ViolationLabelCategory.builder().name(name).build();
        entity.markCreatedBy(accountId.toString());

        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public ViolationLabelCategoryResponseDto update(UUID accountId, UUID categoryId, ViolationLabelCategoryRequestDto request) {
        ViolationLabelCategory entity = findManageableEntity(categoryId);
        String name = request.getName().trim();

        if (!entity.getName().equals(name) && repository.existsByNameAndIsDeletedFalse(name)) {
            throw ContentModuleException.conflict("Nhóm này đã tồn tại: " + name);
        }

        entity.setName(name);
        entity.markUpdatedBy(accountId.toString());

        return toResponse(repository.save(entity));
    }

    @Override
    @Transactional
    public void delete(UUID accountId, UUID categoryId) {
        ViolationLabelCategory entity = findManageableEntity(categoryId);
        // Soft-delete — KHÔNG null hóa category_id ở các ViolationLabelTranslation đang tham
        // chiếu (giữ lịch sử hiển thị), chỉ ẩn nhóm này khỏi danh sách chọn mới (list() lọc
        // isDeleted=false). Khớp cách Tag/Category xử lý xóa ở nơi khác trong codebase.
        entity.softDelete(accountId.toString());
        repository.save(entity);
    }

    private ViolationLabelCategory findManageableEntity(UUID categoryId) {
        return repository.findByCategoryIdAndIsDeletedFalse(categoryId)
                .orElseThrow(() -> ContentModuleException.notFound("Không tìm thấy nhóm: " + categoryId));
    }

    private ViolationLabelCategoryResponseDto toResponse(ViolationLabelCategory entity) {
        return ViolationLabelCategoryResponseDto.builder()
                .categoryId(entity.getCategoryId())
                .name(entity.getName())
                .build();
    }
}
