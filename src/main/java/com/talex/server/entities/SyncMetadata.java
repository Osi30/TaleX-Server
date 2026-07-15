package com.talex.server.entities;

import com.talex.server.enums.SyncType;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;

@Data
@Entity(name = "sync_metadata")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SyncMetadata {
    @Id
    @Enumerated(EnumType.STRING)
    private SyncType syncType;
    private Instant lastSyncTime;
}
