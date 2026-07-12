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
public interface BookmarkAggregationRepository extends JpaRepository<Episode, String> {

    /// Cập nhật tổng bookmark của Tập phim (Episode)
    @Modifying
    @Transactional
    @Query("UPDATE Episode e " +
            "SET e.analyticData.bookmarks = e.analyticData.bookmarks + :delta " +
            "WHERE e.episodeId = :episodeId")
    void updateEpisodeBookmarkCount(
            @Param("episodeId") String episodeId,
            @Param("delta") long delta
    );

    /// Cập nhật tổng bookmark của Chuỗi phim (Series)
    @Modifying
    @Transactional
    @Query("UPDATE Series s " +
            "SET s.analyticData.bookmarks = s.analyticData.bookmarks + :delta " +
            "WHERE s.seriesId = :seriesId")
    void updateSeriesBookmarkCount(
            @Param("seriesId") String seriesId,
            @Param("delta") long delta
    );

    /// Cập nhật tổng bookmark của CampaignEpisode thuộc Chiến dịch
    @Modifying
    @Transactional
    @Query("UPDATE CampaignSeries cs " +
            "SET cs.analyticData.bookmarks = cs.analyticData.bookmarks + :delta " +
            "WHERE cs.series.seriesId = :seriesId")
    void updateCampaignSeriesBookmarkCount(
            @Param("seriesId") String seriesId,
            @Param("delta") long delta
    );

    /// Cập nhật tổng bookmark của Campaign và cộng dồn currentValue nếu EngagementTarget là BOOKMARK
    @Modifying
    @Transactional
    @Query("UPDATE Campaign c " +
            "SET c.analyticData.bookmarks = c.analyticData.bookmarks + :delta, " +
            "c.currentValue = c.currentValue + (CASE WHEN c.engagementTarget = 'BOOKMARK' THEN :delta ELSE 0 END) " +
            "WHERE c.campaignId IN (SELECT cs.campaign.campaignId FROM CampaignSeries cs WHERE cs.series.seriesId = :seriesId)")
    void updateCampaignBookmarkCountAndTarget(
            @Param("seriesId") String seriesId,
            @Param("delta") long delta
    );

    /// Cập nhật tổng bookmark của Kênh/Nhà sáng tạo (Creator)
    @Modifying
    @Transactional
    @Query("UPDATE Creator cr " +
            "SET cr.analyticData.bookmarks = cr.analyticData.bookmarks + :delta " +
            "WHERE cr.creatorId = (SELECT s.creator.creatorId FROM Series s WHERE s.seriesId = :seriesId)")
    void updateCreatorBookmarkCount(
            @Param("seriesId") String seriesId,
            @Param("delta") long delta
    );

    // == Hour Log ==

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO episode_log (episode_log_id, hour_bucket, episode_id, bookmarks) " +
            "VALUES (gen_random_uuid(), :hourBucket, :episodeId, :delta) " +
            "ON CONFLICT (episode_id, hour_bucket) " +
            "DO UPDATE SET bookmarks = COALESCE(episode_log.bookmarks, 0) + :delta", nativeQuery = true)
    void upsertEpisodeLog(
            @Param("episodeId") String episodeId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO series_log (series_log_id, hour_bucket, series_id, bookmarks) " +
            "VALUES (gen_random_uuid(), :hourBucket, :seriesId, :delta) " +
            "ON CONFLICT (series_id, hour_bucket) " +
            "DO UPDATE SET bookmarks = COALESCE(series_log.bookmarks, 0) + :delta", nativeQuery = true)
    void upsertSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_series_log (campaign_series_log_id, hour_bucket, campaign_series_id, bookmarks, likes, views, comments, shares, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_series_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM campaign_series cs " +
            "WHERE cs.series_id = :seriesId " +
            "ON CONFLICT (campaign_series_id, hour_bucket) " +
            "DO UPDATE SET bookmarks = COALESCE(campaign_series_log.bookmarks, 0) + :delta", nativeQuery = true)
    void upsertCampaignSeriesLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO campaign_log (campaign_log_id, hour_bucket, campaign_id, bookmarks, likes, views, comments, shares, watch_time) " +
            "SELECT gen_random_uuid(), :hourBucket, cs.campaign_id, :delta, 0, 0, 0, 0, 0 " +
            "FROM campaign_series cs " +
            "WHERE cs.series_id = :seriesId " +
            "ON CONFLICT (campaign_id, hour_bucket) " +
            "DO UPDATE SET bookmarks = COALESCE(campaign_log.bookmarks, 0) + :delta", nativeQuery = true)
    void upsertCampaignLog(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO creator_log (creator_log_id, hour_bucket, account_id, bookmarks, likes, views, comments, shares, watch_time, follows) " +
            "SELECT gen_random_uuid(), :hourBucket, c.account_id, :delta, 0, 0, 0, 0, 0.0, 0 " +
            "FROM series s " +
            "JOIN creator c ON s.creator_id = c.creator_id " +
            "WHERE s.series_id = :seriesId " +
            "ON CONFLICT (account_id, hour_bucket) " +
            "DO UPDATE SET bookmarks = COALESCE(creator_log.bookmarks, 0) + :delta", nativeQuery = true)
    void upsertCreatorLogBookmarks(
            @Param("seriesId") String seriesId,
            @Param("hourBucket") LocalDateTime hourBucket,
            @Param("delta") long delta
    );

}