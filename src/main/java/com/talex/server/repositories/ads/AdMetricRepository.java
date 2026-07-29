package com.talex.server.repositories.ads;

import com.talex.server.entities.ads.AdMetric;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import java.util.List;

@Repository
public interface AdMetricRepository extends JpaRepository<AdMetric, UUID> {
    Optional<AdMetric> findByCampaign_CampaignIdAndReportDate(UUID campaignId, LocalDate reportDate);
    
    List<AdMetric> findByCampaign_CampaignIdOrderByReportDateAsc(UUID campaignId);
}
