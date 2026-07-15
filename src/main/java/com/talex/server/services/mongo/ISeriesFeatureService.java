package com.talex.server.services.mongo;

import com.talex.server.entities.series.Category;
import com.talex.server.entities.series.Series;
import com.talex.server.entities.series.Tag;

import java.util.Map;

public interface ISeriesFeatureService {
    void saveSeriesMetadata(Series series, Map<String, Category> categories, Map<String, Tag> tags);

    void syncAllSeriesFeatures();
}
