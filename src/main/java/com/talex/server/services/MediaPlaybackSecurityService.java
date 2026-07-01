package com.talex.server.services;

import com.talex.server.dtos.responses.EpisodePlaybackResponseDto;
import com.talex.server.entities.media.Media;

public interface MediaPlaybackSecurityService {
    EpisodePlaybackResponseDto getEpisodePlayback(String episodeId, String viewerId, String ipAddress, String userAgent);

    EpisodePlaybackResponseDto getCreatorEpisodePlayback(String episodeId, String viewerId, String ipAddress, String userAgent);

    void revokeActiveSessions(Media media);

    int expireOldSessions();
}
