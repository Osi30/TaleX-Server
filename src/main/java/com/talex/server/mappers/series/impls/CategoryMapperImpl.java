package com.talex.server.mappers.series.impls;

import com.talex.server.dtos.responses.series.CategoryResponseDto;
import com.talex.server.entities.series.Category;
import com.talex.server.mappers.series.CategoryMapper;
import org.springframework.stereotype.Component;

@Component
public class CategoryMapperImpl implements CategoryMapper {

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
}
