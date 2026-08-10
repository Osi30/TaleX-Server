package com.talex.server.mappers.series.impls;

import com.talex.server.dtos.responses.series.TagResponseDto;
import com.talex.server.entities.series.Tag;
import com.talex.server.mappers.series.TagMapper;
import org.springframework.stereotype.Component;

@Component
public class TagMapperImpl implements TagMapper {

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
}
