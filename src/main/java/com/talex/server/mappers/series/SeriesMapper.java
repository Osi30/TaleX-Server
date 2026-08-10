package com.talex.server.mappers.series;

import com.talex.server.dtos.recommend.SeriesCardResponseDto;
import com.talex.server.dtos.responses.series.SeriesResponseDto;
import com.talex.server.entities.series.Series;

public interface SeriesMapper {
    SeriesCardResponseDto toCardDto(Series series);
    SeriesResponseDto toResponse(Series series);
}
