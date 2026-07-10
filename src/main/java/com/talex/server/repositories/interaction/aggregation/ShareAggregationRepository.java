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
            "SET s.analyticData.shares = s.analyticData.shares + :delta " +
            "WHERE s.seriesId = (SELECT e.season.series.seriesId FROM Episode e WHERE e.episodeId = :episodeId)")
    void updateSeriesShareCountByEpisode(@Param("episodeId") String episodeId, @Param("delta") long delta);

    /// Cập nhật tổng share của CampaignEpisode thuộc Chiến dịch
    @Modifying
    @Transactional
    @Query("UPDATE CampaignEpisode ce " +
            "SET ce.analyticData.shares = ce.analyticData.shares + :delta " +
            "WHERE ce.episode.episodeId = :episodeId")
    void updateCampaignEpisodeShareCount(@Param("episodeId") String episodeId, @Param("delta") long delta);

    /// Cập nhật tổng share của Campaign và cộng dồn mục tiêu nếu EngagementTarget là SHARE
    @Modifying
    @Transactional
    @Query("UPDATE Campaign c " +
            "SET c.analyticData.shares = c.analyticData.shares + :delta, " +
            "c.currentValue = c.currentValue + (CASE WHEN c.engagementTarget = 'SHARE' THEN :delta ELSE 0 END) " +
            "WHERE c.campaignId IN (SELECT ce.campaign.campaignId FROM CampaignEpisode ce WHERE ce.episode.episodeId = :episodeId)")
    void updateCampaignShareCountAndTarget(@Param("episodeId") String episodeId, @Param("delta") long delta);

    /// Cập nhật tổng share của Kênh/Nhà sáng tạo (Creator)
    @Modifying
    @Transactional
    @Query("UPDATE Creator cr " +
            "SET cr.analyticData.shares = cr.analyticData.shares + :delta " +
            "WHERE cr.creatorId = (SELECT e.season.series.creator.creatorId FROM Episode e WHERE e.episodeId = :episodeId)")
    void updateCreatorShareCount(@Param("episodeId") String episodeId, @Param("delta") long delta);

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
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, shares) " +
            "VALUES (gen_random_uuid(), :hourBucket, " +
            "  (SELECT s.series_id FROM episodes e JOIN seasons se ON e.season_id = se.season_id JOIN series s ON se.series_id = s.series_id WHERE e.episode_id = :episodeId), " +
            "  :delta) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(series_log.shares, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_episode_log (campaign_episode_log_id, hour_bucket, campaign_episode_id, bookmarks, likes, views, comments, shares, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_episode_id, 0, 0, 0, 0, :delta, 0 " +
            "FROM campaign_episode ce " +
            "WHERE ce.episode_id = :episodeId " +
            "ON CONFLICT (campaign_episode_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(campaign_episode_log.shares, 0) + :delta", nativeQuery = true)
    void upsertCampaignEpisodeLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, bookmarks, likes, views, comments, shares, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_id, 0, 0, 0, 0, :delta, 0 " +
            "FROM campaign_episode ce " +
            "WHERE ce.episode_id = :episodeId " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET shares = COALESCE(campaign_log.shares, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") long delta);
}
