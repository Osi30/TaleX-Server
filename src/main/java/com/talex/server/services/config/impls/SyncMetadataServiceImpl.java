package com.talex.server.services.config.impls;

import com.talex.server.dtos.responses.config.SyncMetadataResponseDto;
import com.talex.server.enums.SyncType;
import com.talex.server.repositories.SyncMetadataRepository;
import com.talex.server.services.config.SyncMetadataService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;

@Service
@RequiredArgsConstructor
public class SyncMetadataServiceImpl implements SyncMetadataService {
    private final SyncMetadataRepository syncMetadataRepository;

    @Override
    public SyncMetadataResponseDto getSyncMetadata(SyncType syncType) {
        return syncMetadataRepository.findById(syncType)
                .map(meta -> SyncMetadataResponseDto.builder()
                        .syncType(meta.getSyncType())
                        .lastSyncTime(meta.getLastSyncTime() != null ?
                                LocalDateTime.ofInstant(meta.getLastSyncTime(), ZoneId.systemDefault()) : null)
                        .build())
                .orElse(null);
    }
}
