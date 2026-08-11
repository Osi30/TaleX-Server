package com.talex.server.repositories.creator;

import com.talex.server.entities.config.CreatorConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CreatorConfigRepository extends JpaRepository<CreatorConfig, String> {
}