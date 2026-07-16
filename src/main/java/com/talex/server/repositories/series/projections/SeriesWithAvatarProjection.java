package com.talex.server.repositories.series.projections;

import com.talex.server.entities.series.Series;

public interface SeriesWithAvatarProjection {
    Series getSeries();
    String getAvatarUrl();
}
