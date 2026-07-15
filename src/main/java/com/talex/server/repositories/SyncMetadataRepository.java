package com.talex.server.repositories;

import com.talex.server.entities.SyncMetadata;
import com.talex.server.enums.SyncType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SyncMetadataRepository extends JpaRepository<SyncMetadata, SyncType> {
}
