package com.talex.server.services.impls;

import com.talex.server.services.PlaybackAuthorizationService;
import org.springframework.stereotype.Service;

@Service
public class DefaultPlaybackAuthorizationService implements PlaybackAuthorizationService {
    @Override
    public boolean canViewEpisode(String viewerId, String episodeId) {
        return true;
    }
}
