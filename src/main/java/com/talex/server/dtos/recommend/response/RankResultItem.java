package com.talex.server.dtos.recommend.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class RankResultItem {
    private String seriesId;
    private double score;
}
