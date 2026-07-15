package com.talex.server.entities.mongo.userfeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.mongodb.core.mapping.Field;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DynamicPreferences {
    // Toàn thời gian
    // Các bộ đếm cộng dồn
    @Field("genres_clicks_raw") private Map<String, Long> genresClicksRaw = new HashMap<>();
    @Field("genres_watch_time_raw") private Map<String, Double> genresWatchTimeRaw = new HashMap<>();
    @Field("tags_clicks_raw") private Map<String, Long> tagsClicksRaw = new HashMap<>();
    @Field("tags_watch_time_raw") private Map<String, Double> tagsWatchTimeRaw = new HashMap<>();

    // Các bộ tỉ lệ (%)
    @Field("preferred_genres_by_clicks") private Map<String, Double> preferredGenresByClicks = new HashMap<>();
    @Field("preferred_genres_by_watch_time") private Map<String, Double> preferredGenresByWatchTime = new HashMap<>();
    @Field("preferred_tags_by_clicks") private Map<String, Double> preferredTagsByClicks = new HashMap<>();
    @Field("preferred_tags_by_watch_time") private Map<String, Double> preferredTagsByWatchTime = new HashMap<>();

    // 7 ngày qua
    @Field("preferred_genres_by_clicks_last_7d") private Map<String, Double> preferredGenresByClicksLast7d = new HashMap<>();
    @Field("preferred_genres_by_watch_time_last_7d") private Map<String, Double> preferredGenresByWatchTimeLast7d = new HashMap<>();
    @Field("preferred_tags_by_clicks_last_7d") private Map<String, Double> preferredTagsByClicksLast7d = new HashMap<>();
    @Field("preferred_tags_by_watch_time_last_7d") private Map<String, Double> preferredTagsByWatchTimeLast7d = new HashMap<>();

    // 24 giờ qua
    @Field("preferred_genres_by_clicks_last_24h") private Map<String, Double> preferredGenresByClicksLast24h = new HashMap<>();
    @Field("preferred_genres_by_watch_time_last_24h") private Map<String, Double> preferredGenresByWatchTimeLast24h = new HashMap<>();
    @Field("preferred_tags_by_clicks_last_24h") private Map<String, Double> preferredTagsByClicksLast24h = new HashMap<>();
    @Field("preferred_tags_by_watch_time_last_24h") private Map<String, Double> preferredTagsByWatchTimeLast24h = new HashMap<>();
}
