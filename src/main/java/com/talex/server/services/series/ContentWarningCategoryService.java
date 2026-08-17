package com.talex.server.services.series;

import com.talex.server.dtos.requests.series.ContentWarningCategoryCreateRequestDto;
import com.talex.server.dtos.requests.series.ContentWarningCategoryUpdateRequestDto;
import com.talex.server.dtos.responses.series.ContentWarningCategoryResponseDto;

import java.util.List;
import java.util.UUID;

public interface ContentWarningCategoryService {

    // Public — chỉ trả nhóm active, dùng cho form khai báo của Creator.
    List<ContentWarningCategoryResponseDto> listActive();

    // Admin — trả cả nhóm đã ẩn (isActive=false) để CRUD đầy đủ.
    List<ContentWarningCategoryResponseDto> listAll();

    ContentWarningCategoryResponseDto create(UUID accountId, ContentWarningCategoryCreateRequestDto request);

    ContentWarningCategoryResponseDto update(UUID accountId, UUID categoryId, ContentWarningCategoryUpdateRequestDto request);

    void delete(UUID accountId, UUID categoryId);
}
