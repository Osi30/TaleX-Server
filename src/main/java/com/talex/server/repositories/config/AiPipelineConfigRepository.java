package com.talex.server.repositories.config;

import com.talex.server.entities.config.AiPipelineConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface AiPipelineConfigRepository extends JpaRepository<AiPipelineConfig, UUID> {
}
