package com.talex.server.dtos.responses.config;

import com.talex.server.enums.SyncType;
import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class SyncMetadataResponseDto {
    private SyncType syncType;
    private LocalDateTime lastSyncTime;
}