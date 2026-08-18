package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.CampaignSeries;
import com.talex.server.enums.engagement.CampaignStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;

@Repository
public interface CampaignSeriesRepository extends JpaRepository<CampaignSeries, String> {

    List<CampaignSeries> findByCampaign_CampaignId(String campaignId);

    @Query("""
        SELECT cs.series.seriesId
        FROM CampaignSeries cs
        WHERE cs.status = :status
          AND (:isBlacklistEmpty = true OR cs.series.seriesId NOT IN :blacklist)
        ORDER BY cs.totalImpression ASC
    """)
    List<String> findActivePromotedSeriesIds(
            @Param("status") CampaignStatus status,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            Pageable pageable
    );

    @Query("""
        SELECT DISTINCT cs.series.title
        FROM CampaignSeries cs
        WHERE cs.status IN :statuses
          AND cs.series.seriesId IN :checkList
    """)
    List<String> findDuplicatedPromotedSeriesIds(
            @Param("statuses") Collection<CampaignStatus> statuses,
            @Param("checkList") Collection<String> checkList
    );

    @Modifying
    @Query(value = """
        UPDATE campaign_series cs
        SET status = 'COMPLETED'
        FROM campaign c
        WHERE cs.campaign_id = c.campaign_id
          AND c.target_impression IS NOT NULL
          AND c.target_impression > 0
          AND c.current_impression >= c.target_impression
          AND cs.status IN ('RUNNING', 'PAUSED')
        """, nativeQuery = true)
    int autoCompleteReachedCampaignSeries();

    @Modifying
    @Query(value = """
    UPDATE campaign_series
    SET total_impression = COALESCE(total_impression, 0) + 1
    WHERE campaign_series_id IN (:campaignSeriesIds)
""", nativeQuery = true)
    void incrementImpressionsByCampaignSeriesIds(@Param("campaignSeriesIds") Collection<String> campaignSeriesIds);
}