package com.talex.server.repositories.trending;

import com.talex.server.entities.config.TrendingSampleConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TrendingSampleConfigRepository extends JpaRepository<TrendingSampleConfig, String> {
    Optional<TrendingSampleConfig> findFirstBy();
}