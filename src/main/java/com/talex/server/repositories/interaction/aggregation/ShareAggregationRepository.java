package com.talex.server.repositories.interaction.aggregation;

import com.talex.server.entities.series.Episode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface ShareAggregationRepository extends JpaRepository<Episode, String> {

    /// Cập nhật tổng share của Tập phim (Episode)
    @Modifying
    @Transactional
    @Query("UPDATE Episode e " +
            "SET e.analyticData.shares = e.analyticData.shares + :delta " +
            "WHERE e.episodeId = :episodeId")
    void updateEpisodeShareCount(@Param("episodeId") String episodeId, @Param("delta") long delta);

    /// Cập nhật tổng share của Chuỗi phim (Series)
    @Modifying
    @Transactional
    @Query("UPDATE Series s " +
            "SET s.analyticData.shares = s.analyticData.shares + :delta, " +
            "s.is24hSync = false, s.is7dSync = false, s.lastInteractionTime = :lastInteractionTime " +
            "WHERE s.seriesId = :seriesId")
    void updateSeriesShareCount(
            @Param("seriesId") String seriesId,
            @Param("delta") long delta,
            @Param("lastInteractionTime") LocalDateTime lastInteractionTime
    );

    /// Cập nhật tổng share của CampaignSeries thuộc Chiến dịch
    @Modifying
    @Transactional
    @Query("UPDATE CampaignSeries cs " +
            "SET cs.analyticData.shares = cs.analyticData.shares + :delta " +
            "WHERE cs.series.seriesId = :seriesId " +
            "AND cs.campaign.status = 'RUNNING'")
    void updateCampaignSeriesShareCount(@Param("seriesId") String seriesId, @Param("delta") long delta);

    /// Cập nhật tổng share của Campaign và cộng dồn mục tiêu nếu EngagementTarget là SHARE
    @Modifying
    @Transactional
    @Query("UPDATE Campaign c " +
            "SET c.analyticData.shares = c.analyticData.shares + :delta, " +
            "c.currentValue = c.currentValue + :delta " +
            "WHERE c.campaignId IN (SELECT cs.campaign.campaignId FROM CampaignSeries cs WHERE cs.series.seriesId = :seriesId) " +
            "AND c.status = 'RUNNING'")
    void updateCampaignShareCountAndTarget(@Param("seriesId") String seriesId, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "UPDATE creator " +
            "SET shares = COALESCE(shares, 0) + :delta " +
            "WHERE creator_id = (SELECT s.creator_id FROM series s WHERE s.series_id = :seriesId)", nativeQuery = true)
    void updateCreatorShareCount(@Param("seriesId") String seriesId, @Param("delta") long delta);

    // == Hour Log Aggregations ==

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, shares) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(episode_log.shares, 0) + :delta", nativeQuery = true)
    void upsertEpisodeLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, shares, likes, views, comments, bookmarks, watch_time) " +
            "VALUES (gen_random_uuid(), :hourBucket, :seriesId, :delta, 0, 0, 0, 0, 0.0) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(series_log.shares, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_series_log (campaign_series_log_id, hour_bucket, campaign_series_id, shares, likes, views, comments, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_series_id, :delta, 0, 0, 0, 0, 0.0 " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_series_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(campaign_series_log.shares, 0) + :delta", nativeQuery = true)
    void upsertCampaignSeriesLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, shares, likes, views, comments, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_id, :delta, 0, 0, 0, 0, 0.0 " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(campaign_log.shares, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, shares, likes, views, comments, bookmarks, watch_time, follows) " +
            "SELECT gen_random_uuid(), :hourBucket, c.account_id, :delta, 0, 0, 0, 0, 0.0, 0 " +
            "FROM series s " +
            "JOIN creator c ON s.creator_id = c.creator_id " +
            "WHERE s.series_id = :seriesId " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(creator_log.shares, 0) + :delta", nativeQuery = true)
    void upsertCreatorLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);
}
