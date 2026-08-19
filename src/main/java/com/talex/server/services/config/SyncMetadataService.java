package com.talex.server.services.config;

import com.talex.server.dtos.responses.config.SyncMetadataResponseDto;
import com.talex.server.enums.SyncType;

public interface SyncMetadataService {
    SyncMetadataResponseDto getSyncMetadata(SyncType syncType);
}
