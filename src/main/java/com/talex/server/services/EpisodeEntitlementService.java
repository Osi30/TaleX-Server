package com.talex.server.services;

public interface EpisodeEntitlementService {
    boolean hasPlaybackAccess(String viewerId, String episodeId);
}
