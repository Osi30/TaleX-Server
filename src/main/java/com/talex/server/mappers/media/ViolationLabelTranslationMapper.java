package com.talex.server.mappers.media;

import com.talex.server.dtos.responses.media.ViolationLabelTranslationResponseDto;
import com.talex.server.entities.media.ViolationLabelTranslation;

public interface ViolationLabelTranslationMapper {
    ViolationLabelTranslationResponseDto toResponse(ViolationLabelTranslation entity);
}
