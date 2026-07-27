package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdCreative;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AdCreativeRepository extends JpaRepository<AdCreative, UUID> {
    List<AdCreative> findByCampaign_CampaignId(UUID campaignId);
}
