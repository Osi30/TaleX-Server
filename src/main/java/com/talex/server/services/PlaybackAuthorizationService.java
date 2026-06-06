package com.talex.server.services;

public interface PlaybackAuthorizationService {
    boolean canViewEpisode(String viewerId, String episodeId);
}
