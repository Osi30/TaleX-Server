package com.talex.server.services.series;

import com.talex.server.dtos.BasePageResponse;
import com.talex.server.dtos.requests.series.TagRequestDto;
import com.talex.server.dtos.responses.series.TagResponseDto;
import com.talex.server.entities.series.Tag;

public interface TagService {
    TagResponseDto create(TagRequestDto request);

    TagResponseDto getById(String id);

    BasePageResponse<TagResponseDto> list(Integer page, Integer pageSize);

    BasePageResponse<TagResponseDto> listPublic(Integer page, Integer pageSize);

    TagResponseDto update(String id, TagRequestDto request);

    TagResponseDto hide(String id, String actorId);

    TagResponseDto unhide(String id, String actorId);

    void delete(String id, String actorId);

    Tag findActiveEntity(String id);

    Tag findAssignableEntity(String id);

    TagResponseDto toResponse(Tag tag);
}
