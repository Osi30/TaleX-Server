package com.talex.server.mappers.series;

import com.talex.server.dtos.responses.series.TagResponseDto;
import com.talex.server.entities.series.Tag;

public interface TagMapper {
    TagResponseDto toResponse(Tag tag);
}
