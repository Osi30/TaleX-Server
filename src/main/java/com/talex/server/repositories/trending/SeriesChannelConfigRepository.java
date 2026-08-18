package com.talex.server.repositories.trending;

import com.talex.server.entities.config.SeriesChannelConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeriesChannelConfigRepository extends JpaRepository<SeriesChannelConfig, String> {
    Optional<SeriesChannelConfig> findFirstBy();
}