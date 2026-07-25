package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdCampaign;
import com.talex.server.enums.ads.AdCampaignStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AdCampaignRepository extends JpaRepository<AdCampaign, UUID> {
    List<AdCampaign> findByProfile_ProfileIdOrderByCreatedAtDesc(UUID profileId);
    
    List<AdCampaign> findByStatusOrderByCreatedAtDesc(AdCampaignStatus status);
    
    @Query("SELECT c FROM AdCampaign c WHERE c.status = 'ACTIVE' AND c.slot.codeName = :slotCode AND c.currentImpressions < c.targetImpressions AND (c.startDate IS NULL OR c.startDate <= CURRENT_TIMESTAMP) AND (c.endDate IS NULL OR c.endDate >= CURRENT_TIMESTAMP)")
    List<AdCampaign> findActiveCampaignsForSlot(@Param("slotCode") String slotCode);
}
