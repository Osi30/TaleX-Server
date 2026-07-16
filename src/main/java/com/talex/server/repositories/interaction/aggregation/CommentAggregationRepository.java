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
public interface CommentAggregationRepository extends JpaRepository<Episode, String> {

    // === UPSERT CHO CÁC BẢNG TỔNG QUAN ===

    /// Cập nhập tổng comment của chủ kênh (Creator)
    @Modifying
    @Transactional
    @Query(value = "UPDATE creator " +
            "SET comments = COALESCE(comments, 0) + :delta " +
            "WHERE creator_id = (SELECT creator_id FROM series WHERE series_id = :seriesId)", nativeQuery = true)
    void updateCreatorCommentCount(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );

    /// Cập nhập tổng comment của tập (Episode)
    @Modifying
    @Transactional
    @Query(value = "UPDATE episodes " +
            "SET comments = COALESCE(comments, 0) + :delta " +
            "WHERE episode_id = :episodeId", nativeQuery = true)
    void updateEpisodeCommentCount(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /// Cập nhập tổng comment của Series
    @Modifying
    @Transactional
    @Query(value = "UPDATE series " +
            "SET comments = COALESCE(comments, 0) + :delta, " +
            "s.is24hSync = false, s.is7dSync = false, s.lastInteractionTime = :lastInteractionTime " +
            "WHERE series_id = :seriesId", nativeQuery = true)
    void updateSeriesCommentCount(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta,
            @Param("lastInteractionTime") LocalDateTime lastInteractionTime
    );

    /**
     * Cập nhật CampaignSeries đang chạy (RUNNING) liên kết với Series này
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign_series " +
            "SET comments = COALESCE(comments, 0) + :delta " +
            "WHERE series_id = :seriesId " +
            "AND campaign_id IN (SELECT campaign_id FROM campaign WHERE status = 'RUNNING')", nativeQuery = true)
    void updateCampaignSeriesCommentCount(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );

    /**
     * Cập nhật Campaign đang chạy (RUNNING) liên kết với Series này
     * Đồng thời kiểm tra nếu engagement_target = 'COMMENT' thì cập nhật luôn current_value
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign " +
            "SET comments = COALESCE(comments, 0) + :delta, " +
            "current_value = CASE WHEN engagement_target = 'COMMENT' " +
            "THEN COALESCE(current_value, 0) + :delta ELSE current_value END " +
            "WHERE status = 'RUNNING' " +
            "AND campaign_id IN (SELECT cs.campaign_id FROM campaign_series cs WHERE cs.series_id = :seriesId)", nativeQuery = true)
    void updateCampaignCommentCountAndTarget(
            @Param("seriesId") String seriesId,
            @Param("delta") int delta
    );

    // === UPSERT CHO CÁC BẢNG LOG THEO HOUR_BUCKET ===

    /// Upsert cho episode log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, comments) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) DO " +
            "UPDATE SET comments = COALESCE(episode_log.comments, 0) + :delta", nativeQuery = true)
    void upsertEpisodeLog(
            @Param("episodeId") String episodeId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    /// Upsert cho series log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, comments) " +
            "VALUES (gen_random_uuid(), :hourBucket, :seriesId, :delta) " +
            "ON CONFLICT (series_id, hour_bucket) DO " +
            "UPDATE SET comments = COALESCE(series_log.comments, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    /// Upsert cho campaign series log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_series_log (campaign_series_log_id, hour_bucket, campaign_series_id, comments) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_series_id, :delta " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_series_id, hour_bucket) " +
            "DO UPDATE SET comments = COALESCE(campaign_series_log.comments, 0) + :delta", nativeQuery = true)
    void upsertCampaignSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    /// Upsert cho campaign log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, comments) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_id, :delta " +
            "FROM campaign_series cs " +
            "JOIN campaign c ON cs.campaign_id = c.campaign_id " +
            "WHERE cs.series_id = :seriesId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET comments = COALESCE(campaign_log.comments, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") int delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, comments, likes, views, shares, bookmarks, watch_time, follows) " +
            "SELECT gen_random_uuid(), :hourBucket, c.account_id, :delta, 0, 0, 0, 0, 0.0, 0 " +
            "FROM series s " +
            "JOIN creator c ON s.creator_id = c.creator_id " +
            "WHERE s.series_id = :seriesId " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET comments = COALESCE(creator_log.comments, 0) + :delta", nativeQuery = true)
    void upsertCreatorLogComments(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );

}