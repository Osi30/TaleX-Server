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
            "WHERE s.seriesId = (SELECT e.season.series.seriesId " +
            "FROM Episode e WHERE e.episodeId = :episodeId)")
    void updateSeriesWatchTimeByEpisode(@Param("episodeId") String episodeId, @Param("delta") double delta);

    /// Cập nhập thời lượng xem của campaign episode
    @Modifying
    @Transactional
    @Query("UPDATE CampaignEpisode ce " +
            "SET ce.analyticData.watchTime = ce.analyticData.watchTime + :delta " +
            "WHERE ce.episode.episodeId = :episodeId")
    void updateCampaignEpisodeWatchTime(@Param("episodeId") String episodeId, @Param("delta") double delta);

    /// Cập nhập thời lượng xem của campaign
    @Modifying
    @Transactional
    @Query("UPDATE Campaign c " +
            "SET c.analyticData.watchTime = c.analyticData.watchTime + :delta " +
            "WHERE c.campaignId IN (SELECT ce.campaign.campaignId FROM CampaignEpisode ce WHERE ce.episode.episodeId = :episodeId)")
    void updateCampaignWatchTimeAndTarget(
            @Param("episodeId") String episodeId,
            @Param("delta") Double delta
    );

    /// Cập nhập thời lượng xem của kênh nhà sáng tạo
    @Modifying
    @Transactional
    @Query("UPDATE Creator cr " +
            "SET cr.analyticData.watchTime = cr.analyticData.watchTime + :delta " +
            "WHERE cr.creatorId = (SELECT e.season.series.creator.creatorId " +
            "FROM Episode e WHERE e.episodeId = :episodeId)")
    void updateCreatorWatchTime(@Param("episodeId") String episodeId, @Param("delta") double delta);

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
            "VALUES (gen_random_uuid(), :hourBucket, " +
            "  (SELECT s.series_id FROM episodes e JOIN seasons se ON e.season_id = se.season_id JOIN series s ON se.series_id = s.series_id WHERE e.episode_id = :episodeId), " +
            "  :delta) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(series_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertSeriesLogWatchTime(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") double delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_episode_log (campaign_episode_log_id, hour_bucket, campaign_episode_id, bookmarks, likes, views, comments, shares, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_episode_id, 0, 0, 0, 0, 0, :delta " +
            "FROM campaign_episode ce " +
            "WHERE ce.episode_id = :episodeId " +
            "ON CONFLICT (campaign_episode_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(campaign_episode_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertCampaignEpisodeLogWatchTime(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") double delta);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, bookmarks, likes, views, comments, shares, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_id, 0, 0, 0, 0, 0, :delta " +
            "FROM campaign_episode ce " +
            "WHERE ce.episode_id = :episodeId " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET watch_time = COALESCE(campaign_log.watch_time, 0) + :delta", nativeQuery = true)
    void upsertCampaignLogWatchTime(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") double delta);
}