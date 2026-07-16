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
public interface ViewAggregationRepository extends JpaRepository<Episode, String> {

    /// Cập nhật tổng view của Tập phim (Episode)
    @Modifying
    @Transactional
    @Query("UPDATE Episode e " +
            "SET e.analyticData.views = e.analyticData.views + :delta " +
            "WHERE e.episodeId = :episodeId")
    void updateEpisodeViewCount(@Param("episodeId") String episodeId, @Param("delta") long delta);

    /// Cập nhật tổng view của Chuỗi phim (Series)
    @Modifying
    @Transactional
    @Query("UPDATE Series s " +
            "SET s.analyticData.views = s.analyticData.views + :delta, " +
            "s.is24hSync = false, s.is7dSync = false " +
            "WHERE s.seriesId = :seriesId")
    void updateSeriesViewCount(@Param("seriesId") String seriesId, @Param("delta") long delta);

    /// Cập nhật tổng view của CampaignSeries thuộc Chiến dịch
    @Modifying
    @Transactional
    @Query("UPDATE CampaignSeries cs SET cs.analyticData.views = cs.analyticData.views + :delta " +
            "WHERE cs.series.seriesId = :seriesId " +
            "AND cs.campaign.status = 'RUNNING'")
    void updateCampaignSeriesViewCount(@Param("seriesId") String seriesId, @Param("delta") long delta);

    /// Cập nhật tổng view của Campaign và cộng dồn mục tiêu nếu EngagementTarget là VIEW
    @Modifying
    @Transactional
    @Query("UPDATE Campaign c " +
            "SET c.analyticData.views = c.analyticData.views + :delta, " +
            "c.currentValue = c.currentValue + (CASE WHEN c.engagementTarget = 'VIEW' THEN :delta ELSE 0 END) " +
            "WHERE c.status = 'RUNNING' " +
            "AND c.campaignId IN (SELECT cs.campaign.campaignId FROM CampaignSeries cs WHERE cs.series.seriesId = :seriesId)")
    void updateCampaignViewCountAndTarget(@Param("seriesId") String seriesId, @Param("delta") long delta);

    /// Cập nhật tổng view của Kênh/Nhà sáng tạo (Creator)
    @Modifying
    @Transactional
    @Query("UPDATE Creator cr SET cr.analyticData.views = cr.analyticData.views + :delta " +
            "WHERE cr.creatorId = (SELECT s.creator.creatorId FROM Series s WHERE s.seriesId = :seriesId)")
    void updateCreatorViewCount(@Param("seriesId") String seriesId, @Param("delta") long delta);

    // == Hour Log Aggregations ==

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, views) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) " +
            "DO UPDATE SET views = COALESCE(episode_log.views, 0) + :delta", nativeQuery = true)
    void upsertEpisodeLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, views) " +
            "VALUES (gen_random_uuid(), :hourBucket, :seriesId, :delta) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET views = COALESCE(series_log.views, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_series_log (campaign_series_log_id, hour_bucket, campaign_series_id, views) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_series_id, :delta " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_series_id, hour_bucket) " +
            "DO UPDATE SET views = COALESCE(campaign_series_log.views, 0) + :delta", nativeQuery = true)
    void upsertCampaignSeriesLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, views) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_id, :delta " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET views = COALESCE(campaign_log.views, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(@Param("seriesId") String seriesId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, views, likes, comments, shares, bookmarks, watch_time, follows) " +
            "SELECT gen_random_uuid(), :hourBucket, c.account_id, :delta, 0, 0, 0, 0, 0.0, 0 " +
            "FROM series s " +
            "JOIN creator c ON s.creator_id = c.creator_id " +
            "WHERE s.series_id = :seriesId " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET views = COALESCE(creator_log.views, 0) + :delta", nativeQuery = true)
    void upsertCreatorLogViews(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );
}