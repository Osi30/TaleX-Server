package com.talex.server.dtos.requests.series;

import com.talex.server.enums.series.ContentType;

public record SeriesSearchCriteria(
        String keyword,          // đã normalize lowercase + wrap "%...%" ở service, nullable
        ContentType contentType, // nullable = không lọc theo loại nội dung
        String categoryId,       // nullable
        String tagId,            // nullable
        Integer yearFrom,        // nullable, lọc theo năm của releasedUpdateTime
        Integer yearTo,          // nullable
        Long minViews            // nullable
) {
}
