package com.talex.server.services.media;

import com.talex.server.dtos.responses.media.DrmPlaybackConfigDto;
import com.talex.server.entities.media.Media;

public interface DrmLicenseService {
    DrmPlaybackConfigDto getLicenseUrls(Media media, String viewerId);

    String generateLicenseToken(Media media, String viewerId);

    boolean validateLicenseRequest(String token);
}
