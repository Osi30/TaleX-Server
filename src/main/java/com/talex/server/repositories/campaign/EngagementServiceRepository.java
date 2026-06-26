package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.EngagementService;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface EngagementServiceRepository
        extends JpaRepository<EngagementService, String>, JpaSpecificationExecutor<EngagementService> {
}
