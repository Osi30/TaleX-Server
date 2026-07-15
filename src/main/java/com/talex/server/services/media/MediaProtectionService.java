package com.talex.server.services.media;

import com.talex.server.dtos.responses.media.DrmPlaybackConfigDto;
import com.talex.server.entities.media.Media;
import com.talex.server.enums.media.MediaProtectionType;

import java.time.LocalDateTime;

public interface MediaProtectionService {
    MediaProtectionType getProtectionType(Media media);

    String generateSignedPlayback(Media media, LocalDateTime expiresAt);

    DrmPlaybackConfigDto generateDrmPlaybackConfig(Media media, String viewerId);

    void revokePlayback(Media media);
}
