package com.talex.server.records;

import java.time.LocalDateTime;
import java.util.UUID;

public record WatchSessionResponseDto(
        String watchSessionId,
        UUID accountId,
        String creatorId,
        String episodeId,
        LocalDateTime startTime
) {}