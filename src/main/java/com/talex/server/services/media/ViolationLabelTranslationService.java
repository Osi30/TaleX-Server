package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.ViolationLabelTranslationCreateRequestDto;
import com.talex.server.dtos.requests.media.ViolationLabelTranslationUpdateRequestDto;
import com.talex.server.dtos.responses.media.ViolationLabelTranslationResponseDto;

import java.util.List;
import java.util.UUID;

public interface ViolationLabelTranslationService {
    List<ViolationLabelTranslationResponseDto> list();

    ViolationLabelTranslationResponseDto create(UUID accountId, ViolationLabelTranslationCreateRequestDto request);

    ViolationLabelTranslationResponseDto update(UUID accountId, UUID translationId, ViolationLabelTranslationUpdateRequestDto request);

    void delete(UUID accountId, UUID translationId);
}
