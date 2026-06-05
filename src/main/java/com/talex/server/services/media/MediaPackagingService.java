package com.talex.server.services.media;

import com.talex.server.entities.Media;

public interface MediaPackagingService {
    String createHlsPackaging(Media media);

    String createDashPackaging(Media media);

    String getManifestUrl(Media media);
}
