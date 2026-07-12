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
public interface WatchTimeAggregationRepository extends JpaRepository<Episode, String> {

    /// Cập nhập thời lượng xem của tập
    @Modifying
    @Transactional
    @Query("UPDATE Episode e " +
            "SET e.analyticData.watchTime = e.analyticData.watchTime + :delta " +
            "WHERE e.episodeId = :episodeId")
    void updateEpisodeWatchTime(
            @Param("episodeId") String episodeId,
            @Param("delta") double delta
    );

    /// Cập nhập thời lượng xem của series
    @Modifying
    @Transactional
    @Query("UPDATE Series s " +
            "SET s.analyticData.watchTime = s.analyticData.watchTime + :delta " +
            "WHERE s.seriesId = :seriesId")
    void updateSeriesWatchTime(
            @Param("seriesId") String seriesId,
            @Param("delta") double delta
    );

    /// Cập nhập thời lượng xem của campaign series
    @Modifying
    @Transactional
    @Query("UPDATE CampaignSeries cs " +
            "SET cs.analyticData.watchTime = cs.analyticData.watchTime + :delta " +
            "WHERE cs.series.seriesId = :seriesId " +
            "AND cs.campaign.status = 'RUNNING'")
    void updateCampaignSeriesWatchTime(
            @Param("seriesId") String seriesId,
            @Param("delta") double delta
    );

    /// Cập nhập thời lượng xem của campaign
    @Modifying
    @Transactional
    @Query("UPDATE Campaign c " +
            "SET c.analyticData.watchTime = c.analyticData.watchTime + :delta " +
            "WHERE c.status = 'RUNNING' " +
            "AND c.campaignId IN (SELECT cs.campaign.campaignId FROM CampaignSeries cs WHERE cs.series.seriesId = :seriesId)")
    void updateCampaignWatchTimeAndTarget(
            @Param("seriesId") String seriesId,
            @Param("delta") double delta
    );

    /// Cập nhập thời lượng xem của kênh nhà sáng tạo
    @Modifying
    @Transactional
    @Query("UPDATE Creator cr " +
            "SET cr.analyticData.watchTime = cr.analyticData.watchTime + :delta " +
            "WHERE cr.creatorId = (SELECT s.creator.creatorId FROM Series s WHERE s.seriesId = :seriesId)")
    void updateCreatorWatchTime(
            @Param("seriesId") String seriesId,
            @Param("delta") double delta
    );

    // == CẬP NHẬP LOG THEO HOUR ==

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, watch_time) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(episode_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertEpisodeLogWatchTime(
            @Param("episodeId") String episodeId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") double delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, watch_time) " +
            "VALUES (gen_random_uuid(), :hourBucket, :seriesId, :delta) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(series_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") double delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_series_log (campaign_series_log_id, hour_bucket, campaign_series_id, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_series_id, :delta " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_series_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(campaign_series_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertCampaignSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") double delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_id, :delta " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(campaign_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") double delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, watch_time, likes, views, comments, shares, bookmarks, follows) " +
            "SELECT gen_random_uuid(), :hourBucket, c.account_id, :delta, 0, 0, 0, 0, 0, 0 " +
            "FROM series s " +
            "JOIN creator c ON s.creator_id = c.creator_id " +
            "WHERE s.series_id = :seriesId " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(creator_log.watch_time, 0.0) + :delta", nativeQuery = true)
    void upsertCreatorLogWatchTime(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") double delta
    );
}