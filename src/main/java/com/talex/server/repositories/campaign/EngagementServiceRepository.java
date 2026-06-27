package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.EngagementService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EngagementServiceRepository
        extends JpaRepository<EngagementService, String>, JpaSpecificationExecutor<EngagementService> {
    Optional<EngagementService> findByEngagementServiceIdAndIsDeletedFalse(String id);
}
