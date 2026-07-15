package com.talex.server.services.series;

public interface EpisodeEntitlementService {
    boolean hasPlaybackAccess(String viewerId, String episodeId);
}
