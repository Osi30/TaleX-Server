package com.talex.server.dtos.recommend.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PoolSeriesCardResponseDto {
    private String score;
    private SeriesCardResponseDto seriesCard;
}