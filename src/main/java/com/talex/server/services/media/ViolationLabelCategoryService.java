package com.talex.server.services.media;

import com.talex.server.dtos.requests.media.ViolationLabelCategoryRequestDto;
import com.talex.server.dtos.responses.media.ViolationLabelCategoryResponseDto;

import java.util.List;
import java.util.UUID;

public interface ViolationLabelCategoryService {
    List<ViolationLabelCategoryResponseDto> list();

    ViolationLabelCategoryResponseDto create(UUID accountId, ViolationLabelCategoryRequestDto request);

    ViolationLabelCategoryResponseDto update(UUID accountId, UUID categoryId, ViolationLabelCategoryRequestDto request);

    void delete(UUID accountId, UUID categoryId);
}
