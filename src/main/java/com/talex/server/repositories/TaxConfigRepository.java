package com.talex.server.repositories;

import com.talex.server.entities.config.TaxConfig;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TaxConfigRepository extends JpaRepository<TaxConfig, String> {
}