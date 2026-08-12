package com.talex.server.mappers.series;

import com.talex.server.dtos.recommend.response.SeriesCardResponseDto;
import com.talex.server.dtos.responses.series.SeriesResponseDto;
import com.talex.server.dtos.responses.series.SeriesTrendingResponseDto;
import com.talex.server.entities.series.Series;

public interface SeriesMapper {
    SeriesCardResponseDto toCardDto(Series series);
    SeriesTrendingResponseDto toTrendingDto(Series series);
    SeriesResponseDto toResponse(Series series);
}
