package com.talex.server.repositories.config;

import com.talex.server.entities.config.SettlementConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface SettlementConfigRepository extends JpaRepository<SettlementConfig, String> {
}