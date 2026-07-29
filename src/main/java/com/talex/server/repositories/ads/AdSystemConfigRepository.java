package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdSystemConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdSystemConfigRepository extends JpaRepository<AdSystemConfig, UUID> {
    Optional<AdSystemConfig> findByConfigKey(String configKey);
}
