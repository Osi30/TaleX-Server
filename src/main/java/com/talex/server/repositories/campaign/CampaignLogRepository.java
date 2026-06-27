package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.CampaignLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

@Repository
public interface CampaignLogRepository
        extends JpaRepository<CampaignLog, String>, JpaSpecificationExecutor<CampaignLog> {
}
