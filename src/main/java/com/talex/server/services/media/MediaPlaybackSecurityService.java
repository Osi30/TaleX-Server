package com.talex.server.services.media;

import com.talex.server.dtos.responses.series.EpisodePlaybackResponseDto;
import com.talex.server.entities.media.Media;

public interface MediaPlaybackSecurityService {
    EpisodePlaybackResponseDto getEpisodePlayback(String episodeId, String viewerId, String ipAddress, String userAgent);

    EpisodePlaybackResponseDto getCreatorEpisodePlayback(String episodeId, String viewerId, String ipAddress, String userAgent);

    void revokeActiveSessions(Media media);

    int expireOldSessions();
}
