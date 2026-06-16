package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionStat;
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

    @Query("SELECT ser.creatorId " +
            "FROM Episode e " +
            "JOIN e.season sea " +
            "JOIN sea.series ser " +
            "WHERE e.episodeId = :episodeId")
    String findCreatorIdByEpisodeId(@Param("episodeId") String episodeId);

    @Modifying
    @Transactional
    @Query(value = "INSERT INTO subscription_stats " +
            "(month_year, subscription_id, viewer_id, episode_id, creator_id, content_type, is_like, is_comment, is_bookmark, is_share, completion_time) " +
            "VALUES (:monthYear, :subId, :viewerId, :episodeId, :creatorId, :contentType, :isLike, :isComment, :isBookmark, :isShare, 0) " +
            "ON CONFLICT (month_year, subscription_id, viewer_id, episode_id) " +
            "DO UPDATE SET " +
            "  is_like = CASE WHEN EXCLUDED.is_like = true THEN true ELSE subscription_stats.is_like END, " +
            "  is_comment = CASE WHEN EXCLUDED.is_comment = true THEN true ELSE subscription_stats.is_comment END, " +
            "  is_bookmark = CASE WHEN EXCLUDED.is_bookmark = true THEN true ELSE subscription_stats.is_bookmark END, " +
            "  is_share = CASE WHEN EXCLUDED.is_share = true THEN true ELSE subscription_stats.is_share END",
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
            @Param("isShare") boolean isShare
    );
}