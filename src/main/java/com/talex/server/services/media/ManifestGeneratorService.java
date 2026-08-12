package com.talex.server.services.media;

public interface ManifestGeneratorService {
    String generateDynamicManifest(String episodeId, String viewerId);
}
