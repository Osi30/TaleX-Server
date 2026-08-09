package com.talex.server.repositories.media;

import com.talex.server.entities.media.MediaSystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface MediaSystemConfigRepository extends JpaRepository<MediaSystemConfig, UUID> {
}
