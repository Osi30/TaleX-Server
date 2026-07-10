package com.talex.server.services.impls;

import com.talex.server.services.EpisodeEntitlementService;
import com.talex.server.services.PlaybackAuthorizationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class DefaultPlaybackAuthorizationService implements PlaybackAuthorizationService {

    private final EpisodeEntitlementService episodeEntitlementService;

    @Override
    public boolean canViewEpisode(String viewerId, String episodeId) {
        return episodeEntitlementService.hasPlaybackAccess(viewerId, episodeId);
    }
}
