package com.talex.server.services.impls;

import com.talex.server.enums.MediaUploadSessionStatus;

record CachedMediaUploadProgress(
        Long uploadedBytes,
        Integer lastUploadedChunkIndex,
        MediaUploadSessionStatus status,
        String actorId) {
}
