package com.talex.server.services;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.CategoryRequestDto;
import com.talex.server.dtos.responses.CategoryResponseDto;
import com.talex.server.entities.Category;

public interface CategoryService {
    CategoryResponseDto create(CategoryRequestDto request);

    CategoryResponseDto getById(String id);

    BasePageResponse<CategoryResponseDto> list(Integer page, Integer pageSize);

    BasePageResponse<CategoryResponseDto> listPublic(Integer page, Integer pageSize);

    CategoryResponseDto update(String id, CategoryRequestDto request);

    CategoryResponseDto hide(String id, String actorId);

    CategoryResponseDto unhide(String id, String actorId);

    void delete(String id, String actorId);

    Category findActiveEntity(String id);

    Category findAssignableEntity(String id);

    CategoryResponseDto toResponse(Category category);
}
