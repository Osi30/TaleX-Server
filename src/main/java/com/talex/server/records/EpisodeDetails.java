package com.talex.server.records;

import com.talex.server.enums.series.ContentType;

public record EpisodeDetails(
        String creatorId,
        Double totalDuration,
        ContentType contentType
) {
}
