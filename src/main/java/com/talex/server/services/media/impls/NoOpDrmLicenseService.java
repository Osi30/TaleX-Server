package com.talex.server.services.media.impls;

import com.talex.server.dtos.responses.media.DrmPlaybackConfigDto;
import com.talex.server.entities.media.Media;
import com.talex.server.exceptions.details.ContentModuleException;
import com.talex.server.services.media.DrmLicenseService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class NoOpDrmLicenseService implements DrmLicenseService {
    @Override
    public DrmPlaybackConfigDto getLicenseUrls(Media media, String viewerId) {
        log.warn("DRM config requested but no DRM provider is configured. mediaId={}", media.getMediaId());
        throw ContentModuleException.badRequest("DRM_NOT_CONFIGURED");
    }

    @Override
    public String generateLicenseToken(Media media, String viewerId) {
        throw ContentModuleException.badRequest("DRM_NOT_CONFIGURED");
    }

    @Override
    public boolean validateLicenseRequest(String token) {
        return false;
    }
}
