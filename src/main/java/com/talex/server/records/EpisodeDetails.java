package com.talex.server.records;

import com.talex.server.enums.ContentType;

public record EpisodeDetails(
        String creatorId,
        Double totalDuration,
        ContentType contentType
) {
}
