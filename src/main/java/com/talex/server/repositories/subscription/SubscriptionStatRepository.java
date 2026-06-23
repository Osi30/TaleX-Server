package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionStat;
import com.talex.server.records.EpisodeDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Repository
public interface SubscriptionStatRepository extends JpaRepository<SubscriptionStat, String> {

    @Query("SELECT sub.accountSubscriptionId " +
            "FROM Account a " +
            "JOIN a.accountSubscriptions sub " +
            "WHERE a.accountId = :accountId " +
            "AND sub.isCancelled = false " +
            "AND sub.endTime >= :timestamp " +
            "AND sub.startTime <= :timestamp")
    String findActiveAccountSubByAccountId(
            @Param("accountId") UUID accountId,
            @Param("timestamp") LocalDateTime timestamp
    );

    @Query("SELECT ser.creatorId, e.totalDuration, e.contentType " +
            "FROM Episode e " +
            "JOIN e.season sea " +
            "JOIN sea.series ser " +
            "WHERE e.episodeId = :episodeId")
    EpisodeDetails findEpisodeDetails(@Param("episodeId") String episodeId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO subscription_stats " +
            "(id, month_year, subscription_id, viewer_id, episode_id, creator_id, content_type, is_like, is_comment, is_bookmark, is_share, is_repeat, last_session_id, total_time, completion_time) " +
            "VALUES (gen_random_uuid(), :monthYear, :subId, :viewerId, :episodeId, :creatorId, :contentType, :isLike, :isComment, :isBookmark, :isShare, false, :sessionId, :totalTime, 0) " +
            "ON CONFLICT (month_year, subscription_id, viewer_id, episode_id) " +
            "DO UPDATE SET " +
            "  is_like = CASE " +
            "    WHEN :interactionType = 'LIKE' THEN true " +
            "    WHEN :interactionType = 'UNLIKE' THEN false " +
            "    ELSE subscription_stats.is_like " +
            "  END, " +
            "  is_bookmark = CASE " +
            "    WHEN :interactionType = 'BOOKMARK' THEN true " +
            "    WHEN :interactionType = 'UNBOOKMARK' THEN false " +
            "    ELSE subscription_stats.is_bookmark " +
            "  END, " +
            "  is_comment = CASE WHEN EXCLUDED.is_comment = true THEN true ELSE subscription_stats.is_comment END, " +
            "  is_share = CASE WHEN EXCLUDED.is_share = true THEN true ELSE subscription_stats.is_share END, " +
            "  is_repeat = CASE " +
            "    WHEN subscription_stats.is_repeat = true THEN true " +
            "    WHEN subscription_stats.last_session_id <> EXCLUDED.last_session_id THEN true " +
            "    ELSE false " +
            "  END, " +
            "  last_session_id = EXCLUDED.last_session_id, " +
            "  total_time = EXCLUDED.total_time",
            nativeQuery = true)
    void upsertInteractionFlags(
            @Param("monthYear") String monthYear,
            @Param("subId") String subId,
            @Param("viewerId") String viewerId,
            @Param("episodeId") String episodeId,
            @Param("contentType") String contentType,
            @Param("creatorId") String creatorId,
            @Param("isLike") boolean isLike,
            @Param("isComment") boolean isComment,
            @Param("isBookmark") boolean isBookmark,
            @Param("isShare") boolean isShare,
            @Param("sessionId") String sessionId,
            @Param("totalTime") Double totalTime,
            @Param("interactionType") String interactionType
    );

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO subscription_stats (id, month_year, subscription_id, viewer_id, episode_id, creator_id, content_type, completion_time, last_session_id) " +
            "VALUES (gen_random_uuid(), :monthYear, :subId, :viewerId, :episodeId, :creatorId, :contentType, :duration, :sessionId) " +
            "ON CONFLICT (month_year, subscription_id, viewer_id, episode_id) " +
            "DO UPDATE SET completion_time = subscription_stats.completion_time + EXCLUDED.completion_time, " +
            "  is_repeat = CASE " +
            "    WHEN subscription_stats.is_repeat = true THEN true " +
            "    WHEN subscription_stats.last_session_id <> EXCLUDED.last_session_id THEN true " +
            "    ELSE false " +
            "  END, " +
            "  last_session_id = EXCLUDED.last_session_id",
            nativeQuery = true)
    void upsertWatchTime(
            @Param("monthYear") String monthYear,
            @Param("subId") String subId,
            @Param("viewerId") String viewerId,
            @Param("episodeId") String episodeId,
            @Param("contentType") String contentType,
            @Param("creatorId") String creatorId,
            @Param("duration") Double duration,
            @Param("sessionId") String sessionId
    );
}