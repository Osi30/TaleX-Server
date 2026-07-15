package com.talex.server.services.auth;

public interface PlaybackAuthorizationService {
    boolean canViewEpisode(String viewerId, String episodeId);
}
