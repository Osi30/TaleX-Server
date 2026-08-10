package com.talex.server.mappers.series;

import com.talex.server.dtos.responses.series.CategoryResponseDto;
import com.talex.server.entities.series.Category;

public interface CategoryMapper {
    CategoryResponseDto toResponse(Category category);
}
