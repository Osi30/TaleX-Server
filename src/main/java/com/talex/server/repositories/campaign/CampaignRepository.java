package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.Campaign;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;

@Repository
public interface CampaignRepository extends JpaRepository<Campaign, String>, JpaSpecificationExecutor<Campaign> {

    @Modifying
    @Query(value = """
    UPDATE campaign c
    SET current_impression = COALESCE(c.current_impression, 0) + sub.cnt,
        end_at = CASE 
            WHEN (COALESCE(c.current_impression, 0) + sub.cnt) >= c.target_impression THEN :now 
            ELSE c.end_at 
        END
    FROM (
        SELECT cs.campaign_id, COUNT(*) AS cnt
        FROM campaign_series cs
        WHERE cs.campaign_series_id IN (:campaignSeriesIds)
        GROUP BY cs.campaign_id
    ) sub
    WHERE c.campaign_id = sub.campaign_id
""", nativeQuery = true)
    void incrementCampaignImpressionsByCampaignSeriesIds(
            @Param("campaignSeriesIds") Collection<String> campaignSeriesIds,
            @Param("now") LocalDateTime now
    );
}
