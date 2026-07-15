package com.talex.server.services.media;

public interface MediaCleanupService {
    int expireStaleUploadSessions();

    int expirePlaybackSessions();
}
