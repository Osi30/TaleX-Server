package com.talex.server.services;

public interface MediaCleanupService {
    int expireStaleUploadSessions();

    int expirePlaybackSessions();
}
