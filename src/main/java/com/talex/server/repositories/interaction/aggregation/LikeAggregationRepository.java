package com.talex.server.repositories.interaction.aggregation;

import com.talex.server.entities.interaction.AccountLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;

@Repository
public interface LikeAggregationRepository extends JpaRepository<AccountLike, String> {

    // --- CẬP NHẬT TỔNG LƯỢT LIKE THỜI GIAN THỰC ---

    /// Cập nhập cho tập phim
    @Modifying
    @Query(value = "UPDATE episodes " +
            "SET likes = likes + :delta " +
            "WHERE episode_id = :episodeId", nativeQuery = true)
    void updateEpisodeLikeCount(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /// Cập nhập cho series
    @Modifying
    @Query(value = "UPDATE series SET likes = likes + :delta " +
            "WHERE series_id = (SELECT sea.series_id FROM episodes e " +
            "JOIN seasons sea ON e.season_id = sea.season_id WHERE e.episode_id = :episodeId)", nativeQuery = true)
    void updateSeriesLikeCountByEpisode(
            @Param("episodeId") String episodeId, 
            @Param("delta") int delta
    );
    
    /// Cập nhập cho campaign của tập (nếu có)
    @Modifying
    @Query(value = "UPDATE campaign_episode " +
            "SET likes = likes + :delta " +
            "WHERE episode_id = :episodeId", nativeQuery = true)
    void updateCampaignEpisodeLikeCount(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /// Cập nhập cho campaign chung (nếu có)
    @Modifying
    @Query(value = "UPDATE campaign SET likes = likes + :delta, " +
            "current_value = current_value + CASE WHEN engagement_target = 'LIKE' THEN :delta ELSE 0 END " +
            "WHERE campaign_id IN (SELECT campaign_id FROM campaign_episode WHERE episode_id = :episodeId)", nativeQuery = true)
    void updateCampaignLikeCountAndTarget(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /// Cập nhập chung cho creator
    @Modifying
    @Query(value = "UPDATE creator SET likes = likes + :delta " +
            "WHERE creator_id = (SELECT s.creator_id FROM episodes e " +
            "JOIN seasons sea ON e.season_id = sea.season_id " +
            "JOIN series s ON sea.series_id = s.series_id WHERE e.episode_id = :episodeId)", nativeQuery = true)
    void updateCreatorLikeCount(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );


    // --- UPSERT CÁC BẢNG LOG THEO HOUR BUCKET ---

    @Modifying
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, likes) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) " +
            "DO UPDATE SET likes = episode_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertEpisodeLog(
            @Param("episodeId") String episodeId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    @Modifying
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, likes, views, comments, shares, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, sea.series_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM episodes e JOIN seasons sea ON e.season_id = sea.season_id WHERE e.episode_id = :episodeId " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET likes = series_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertSeriesLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);

    @Modifying
    @Query(value = "INSERT INTO campaign_episode_log (campaign_episode_log_id, hour_bucket, campaign_episode_id, likes, views, comments, shares, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_episode_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM campaign_episode ce WHERE ce.episode_id = :episodeId " +
            "ON CONFLICT (campaign_episode_id, hour_bucket) " +
            "DO UPDATE SET likes = campaign_episode_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertCampaignEpisodeLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);

    @Modifying
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, likes, views, comments, shares, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM campaign_episode ce WHERE ce.episode_id = :episodeId " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET likes = campaign_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertCampaignLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);
}