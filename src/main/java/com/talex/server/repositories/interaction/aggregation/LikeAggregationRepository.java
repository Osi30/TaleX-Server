package com.talex.server.repositories.interaction.aggregation;

import com.talex.server.entities.interaction.AccountLike;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Repository
public interface LikeAggregationRepository extends JpaRepository<AccountLike, String> {

    // --- CẬP NHẬT TỔNG LƯỢT LIKE THỜI GIAN THỰC ---

    /// Cập nhập cho tập phim
    @Modifying
    @Transactional
    @Query(value = "UPDATE episodes " +
            "SET likes = likes + :delta " +
            "WHERE episode_id = :episodeId", nativeQuery = true)
    void updateEpisodeLikeCount(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /// Cập nhập cho series
    @Modifying
    @Transactional
    @Query(value = "UPDATE series SET likes = likes + :delta " +
            "WHERE series_id = :seriesId", nativeQuery = true)
    void updateSeriesLikeCount(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );
    
    /// Cập nhập cho campaign của series (nếu có)
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign_series " +
            "SET likes = likes + :delta " +
            "WHERE series_id = :seriesId", nativeQuery = true)
    void updateCampaignSeriesLikeCount(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );

    /// Cập nhập cho campaign chung (nếu có)
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign " +
            "SET likes = likes + :delta, " +
            "current_value = current_value + CASE WHEN engagement_target = 'LIKE' THEN :delta ELSE 0 END " +
            "WHERE campaign_id IN (SELECT cs.campaign_id FROM campaign_series cs WHERE cs.series_id = :seriesId)", nativeQuery = true)
    void updateCampaignLikeCountAndTarget(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );

    /// Cập nhập chung cho creator
    @Modifying
    @Transactional
    @Query(value = "UPDATE creator SET likes = likes + :delta " +
            "WHERE creator_id = (SELECT creator_id FROM series WHERE series_id = :seriesId)", nativeQuery = true)
    void updateCreatorLikeCount(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );


    // --- UPSERT CÁC BẢNG LOG THEO HOUR BUCKET ---

    @Modifying
    @Transactional
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
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, likes, views, comments, shares, bookmarks, watch_time) " +
            "VALUES (gen_random_uuid(), :hourBucket, :seriesId, :delta, 0, 0, 0, 0, 0) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET likes = series_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_series_log (campaign_series_log_id, hour_bucket, campaign_series_id, likes, views, comments, shares, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_series_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM campaign_series cs WHERE cs.series_id = :seriesId " +
            "ON CONFLICT (campaign_series_id, hour_bucket) " +
            "DO UPDATE SET likes = campaign_series_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertCampaignSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, likes, views, comments, shares, bookmarks, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM campaign_series cs WHERE cs.series_id = :seriesId " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET likes = campaign_log.likes + EXCLUDED.likes", nativeQuery = true)
    void upsertCampaignLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, likes, views, comments, shares, bookmarks, watch_time, follows) " +
            "SELECT gen_random_uuid(), :hourBucket, c.account_id, :delta, 0, 0, 0, 0, 0.0, 0 " +
            "FROM series s " +
            "JOIN creator c ON s.creator_id = c.creator_id " +
            "WHERE s.series_id = :seriesId " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET likes = COALESCE(creator_log.likes, 0) + :delta", nativeQuery = true)
    void upsertCreatorLogLikes(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );
}