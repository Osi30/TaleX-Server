package com.talex.server.services.interaction;

import java.util.UUID;

public interface IAccountShareService {
    void shareEpisode(UUID accountId, String episodeId);
}
