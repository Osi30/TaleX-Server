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
            "WHERE creator_id = (SELECT creator_id FROM episodes WHERE episode_id = :episodeId)", nativeQuery = true)
    void updateCreatorCommentCount(
            @Param("episodeId") String episodeId,
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
            "SET comments = COALESCE(comments, 0) + :delta " +
            "WHERE series_id = " +
            "(SELECT s.series_id " +
            "FROM episodes e " +
            "JOIN seasons se ON e.season_id = se.season_id " +
            "JOIN series s ON se.series_id = s.series_id " +
            "WHERE e.episode_id = :episodeId)", nativeQuery = true)
    void updateSeriesCommentCountByEpisode(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /**
     * Cập nhật CampaignEpisode đang chạy (RUNNING) liên kết với Episode này
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign_episode " +
            "SET comments = COALESCE(comments, 0) + :delta " +
            "WHERE episode_id = :episodeId " +
            "AND campaign_id IN " +
            "(SELECT campaign_id " +
            "FROM campaign " +
            "WHERE status = 'RUNNING')",
            nativeQuery = true)
    void updateCampaignEpisodeCommentCount(
            @Param("episodeId") String episodeId,
            @Param("delta") int delta
    );

    /**
     * Cập nhật Campaign đang chạy (RUNNING) liên kết với Episode này
     * Đồng thời kiểm tra nếu engagement_target = 'COMMENT' thì cập nhật luôn current_value
     **/
    @Modifying
    @Transactional
    @Query(value = "UPDATE campaign " +
            "SET comments = COALESCE(comments, 0) + :delta, " +
            "current_value = CASE WHEN engagement_target = 'COMMENT' " +
            "THEN COALESCE(current_value, 0) + :delta ELSE current_value END " +
            "WHERE status = 'RUNNING' AND campaign_id IN (" +
            "SELECT campaign_id " +
            "FROM campaign_episode " +
            "WHERE episode_id = :episodeId)", nativeQuery = true)
    void updateCampaignCommentCountAndTarget(@Param("episodeId") String episodeId, @Param("delta") int delta);


    // === UPSERT CHO CÁC BẢNG LOG THEO HOUR_BUCKET ===

    /// Upsert cho episode log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, comments) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) DO " +
            "UPDATE SET comments = COALESCE(episode_log.comments, 0) + :delta", nativeQuery = true)
    void upsertEpisodeLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);

    /// Upsert cho series log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, comments) " +
            "VALUES (gen_random_uuid(), :hourBucket, (SELECT s.series_id FROM episodes e JOIN seasons se ON e.season_id = se.season_id JOIN series s ON se.series_id = s.series_id WHERE e.episode_id = :episodeId), :delta) " +
            "ON CONFLICT (series_id, hour_bucket) DO " +
            "UPDATE SET comments = COALESCE(series_log.comments, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);

    /// Upsert cho campaign episode log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_episode_log (campaign_episode_log_id, hour_bucket, campaign_episode_id, comments) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_episode_id, :delta FROM campaign_episode ce JOIN campaign c ON ce.campaign_id = c.campaign_id WHERE ce.episode_id = :episodeId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_episode_id, hour_bucket) DO UPDATE SET comments = COALESCE(campaign_episode_log.comments, 0) + :delta", nativeQuery = true)
    void upsertCampaignEpisodeLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);

    /// Upsert cho campaign log theo hour bucket
    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, comments) " +
            "SELECT gen_random_uuid(), :hourBucket, ce.campaign_id, :delta FROM campaign_episode ce JOIN campaign c ON ce.campaign_id = c.campaign_id WHERE ce.episode_id = :episodeId AND c.status = 'RUNNING' " +
            "ON CONFLICT (campaign_id, hour_bucket) DO UPDATE SET comments = COALESCE(campaign_log.comments, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(@Param("episodeId") String episodeId, @Param("hourBucket") LocalDateTime hourBucket, @Param("delta") int delta);

}