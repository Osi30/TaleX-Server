package com.talex.server.dtos.interaction;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EpisodeHourKey {
    private String episodeId;
    private LocalDateTime hourBucket;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EpisodeHourKey that = (EpisodeHourKey) o;
        return Objects.equals(episodeId, that.episodeId) && Objects.equals(hourBucket, that.hourBucket);
    }

    @Override
    public int hashCode() {
        return Objects.hash(episodeId, hourBucket);
    }
}