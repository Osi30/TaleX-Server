package com.talex.server.dtos.requests.series;

import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.SeriesStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SeriesSearchCriteria {
    private String seriesId;
    private String search;
    private ContentType contentType;
    private List<String> ageRatings;
    private SeriesStatus status;
    private List<String> categoryIds;
    private List<String> tagIds;
    private String sortBy;
    private String sortDirection;
}