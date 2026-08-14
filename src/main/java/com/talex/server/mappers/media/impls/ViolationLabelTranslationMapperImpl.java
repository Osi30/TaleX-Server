package com.talex.server.mappers.media.impls;

import com.talex.server.dtos.responses.media.ViolationLabelTranslationResponseDto;
import com.talex.server.entities.media.ViolationLabelTranslation;
import com.talex.server.mappers.media.ViolationLabelTranslationMapper;
import org.springframework.stereotype.Component;

@Component
public class ViolationLabelTranslationMapperImpl implements ViolationLabelTranslationMapper {

    @Override
    public ViolationLabelTranslationResponseDto toResponse(ViolationLabelTranslation entity) {
        return ViolationLabelTranslationResponseDto.builder()
                .translationId(entity.getTranslationId())
                .awsLabel(entity.getAwsLabel())
                .vietnameseText(entity.getVietnameseText())
                .categoryId(entity.getCategory() != null ? entity.getCategory().getCategoryId() : null)
                .categoryName(entity.getCategory() != null ? entity.getCategory().getName() : null)
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
