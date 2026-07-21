package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.CampaignSeries;
import com.talex.server.enums.engagement.CampaignStatus;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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
}