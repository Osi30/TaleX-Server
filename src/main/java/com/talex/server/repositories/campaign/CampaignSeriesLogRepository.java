package com.talex.server.repositories.campaign;

import com.talex.server.entities.campaign.CampaignSeriesLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface CampaignSeriesLogRepository extends JpaRepository<CampaignSeriesLog, String> {

    List<CampaignSeriesLog> findByCampaignSeries_CampaignSeriesIdAndHourBucketBetweenOrderByHourBucketAsc(
            String campaignSeriesId,
            LocalDateTime startTime,
            LocalDateTime endTime
    );

    @Modifying
    @Query(value = """
    INSERT INTO campaign_series_log (campaign_series_log_id, campaign_series_id, hour_bucket, total_impression)
    SELECT gen_random_uuid(), cs.campaign_series_id, :hourBucket, 1
    FROM campaign_series cs
    WHERE cs.campaign_series_id IN (:campaignSeriesIds)
    ON CONFLICT (campaign_series_id, hour_bucket)
    DO UPDATE SET total_impression = campaign_series_log.total_impression + 1
""", nativeQuery = true)
    void upsertBatchLogImpressionsByCampaignSeriesIds(
            @Param("campaignSeriesIds") Collection<String> campaignSeriesIds,
            @Param("hourBucket") LocalDateTime hourBucket
    );
}