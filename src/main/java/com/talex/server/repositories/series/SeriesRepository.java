package com.talex.server.repositories.series;

import com.talex.server.entities.series.Series;
import com.talex.server.enums.series.SeriesStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, String> {
    Optional<Series> findBySeriesIdAndIsDeletedFalse(String seriesId);

    Page<Series> findAllByIsDeletedFalse(Pageable pageable);

    Page<Series> findAllByCreator_CreatorIdAndIsDeletedFalse(String creatorId, Pageable pageable);

    Page<Series> findAllByStatusAndIsDeletedFalse(
            SeriesStatus status,
            Pageable pageable);

    Page<Series> findAllByStatusInAndIsDeletedFalse(
            Collection<SeriesStatus> statuses,
            Pageable pageable);

    @Query("SELECT s AS series, a.avatarUrl AS avatarUrl, a.fullName AS creatorFullName, a.username AS creatorUsername " +
            "FROM Series s " +
            "JOIN s.creator c " +
            "JOIN c.account a " +
            "WHERE s.status IN :statuses AND s.isDeleted = false")
    Page<com.talex.server.repositories.series.projections.SeriesWithAvatarProjection> findPublicSeriesWithAvatar(
            @Param("statuses") Collection<SeriesStatus> statuses,
            Pageable pageable);

    @Query("SELECT s AS series, a.avatarUrl AS avatarUrl, a.fullName AS creatorFullName, a.username AS creatorUsername " +
            "FROM Series s " +
            "JOIN s.creator c " +
            "JOIN c.account a " +
            "WHERE s.seriesId = :seriesId AND s.isDeleted = false")
    Optional<com.talex.server.repositories.series.projections.SeriesWithAvatarProjection> findActiveSeriesWithAvatarById(
            @Param("seriesId") String seriesId);

    long countBySeriesIdInAndStatusAndIsDeletedFalseAndCreator_CreatorId(Collection<String> seriesIds, SeriesStatus status, String creatorId);

    /// Tìm các Series ngừng tương tác quá 24h nhưng chưa reset cụm 24h
    @Query("SELECT s.seriesId " +
            "FROM Series s " +
            "WHERE s.lastInteractionTime < :threshold " +
            "AND s.is24hSync = false")
    List<String> findSeriesIdsFor24hReset(
            @Param("threshold") LocalDateTime threshold
    );

    /// Tìm các Series ngừng tương tác quá 7 ngày nhưng chưa reset cụm 7d
    @Query("SELECT s.seriesId " +
            "FROM Series s " +
            "WHERE s.lastInteractionTime < :threshold " +
            "AND s.is7dSync = false")
    List<String> findSeriesIdsFor7dReset(
            @Param("threshold") LocalDateTime threshold
    );

    /// Update flag sau khi đã reset thành công
    @Modifying
    @Transactional
    @Query("UPDATE Series s " +
            "SET s.is24hSync = true " +
            "WHERE s.seriesId IN :ids")
    void markAs24hSynced(@Param("ids") List<String> ids);

    @Modifying
    @Transactional
    @Query("UPDATE Series s " +
            "SET s.is7dSync = true " +
            "WHERE s.seriesId IN :ids")
    void markAs7dSynced(@Param("ids") List<String> ids);

    @Query("""
        SELECT s.seriesId
        FROM Series s
        WHERE s.isDeleted = false
          AND s.status = :status
          AND s.totalImpression <= :maxImpression
          AND (:isBlacklistEmpty = true OR s.seriesId NOT IN :blacklist)
        ORDER BY s.totalImpression ASC, s.createdAt ASC
    """)
    List<String> findCandidateNewReleasesSeriesIds(
            @Param("status") SeriesStatus status,
            @Param("maxImpression") Long maxImpression,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            Pageable pageable
    );

    @Query("""
        SELECT s.seriesId
        FROM Series s
        WHERE s.isDeleted = false
          AND s.status = :status
          AND (:isBlacklistEmpty = true OR s.seriesId NOT IN :blacklist)
        ORDER BY s.releasedUpdateTime DESC
    """)
    List<String> findCandidateRecentlyUpdatedSeriesIds(
            @Param("status") SeriesStatus status,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            Pageable pageable
    );

    @Query("""
    SELECT s.seriesId
    FROM Series s
    WHERE s.isDeleted = false
      AND s.status = :status
      AND (:isBlacklistEmpty = true OR s.seriesId NOT IN :blacklist)
    ORDER BY s.analyticData.watchTime DESC,
             s.analyticData.likes DESC,
             s.analyticData.views DESC,
             s.analyticData.comments DESC,
             s.analyticData.shares DESC,
             s.analyticData.bookmarks DESC
    """)
    List<String> findCandidateCommunityChoiceSeriesIds(
            @Param("status") SeriesStatus status,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            Pageable pageable
    );

    @Query(value = """
    WITH RankedSeries AS (
        SELECT 
            sc.category_id,
            s.series_id,
            ROW_NUMBER() OVER (
                PARTITION BY sc.category_id 
                ORDER BY 
                    s.watch_time DESC, 
                    s.likes DESC, 
                    s.views DESC, 
                    s.comments DESC, 
                    s.shares DESC, 
                    s.bookmarks DESC
            ) AS rn
        FROM series s
        JOIN series_categories sc ON s.series_id = sc.series_id
        JOIN categories c ON sc.category_id = c.category_id
        WHERE s.is_deleted = false
          AND s.status = :status
          AND sc.is_deleted = false
          AND c.is_deleted = false
          AND c.status = :categoryStatus
          AND (:isBlacklistEmpty = true OR s.series_id NOT IN (:blacklist))
    )
    SELECT DISTINCT series_id
    FROM RankedSeries
    WHERE rn <= :limitPerCategory
    """, nativeQuery = true)
    List<String> findTopSeriesPerCategory(
            @Param("status") String status,
            @Param("categoryStatus") String categoryStatus,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            @Param("limitPerCategory") int limitPerCategory
    );

    @Query(value = """
    WITH RankedSeries AS (
        SELECT 
            c.creator_id,
            s.series_id,
            ROW_NUMBER() OVER (
                PARTITION BY c.creator_id 
                ORDER BY 
                    s.released_update_time DESC,
                    s.watch_time DESC, 
                    s.likes DESC, 
                    s.views DESC, 
                    s.comments DESC, 
                    s.shares DESC, 
                    s.bookmarks DESC
            ) AS rn
        FROM account_follow af
        JOIN creator c ON af.followed_id = c.account_id
        JOIN series s ON c.creator_id = s.creator_id
        WHERE CAST(af.follower_id AS text) = :accountId
          AND s.is_deleted = false
          AND s.status = :status
          AND (:isBlacklistEmpty = true OR s.series_id NOT IN (:blacklist))
    )
    SELECT DISTINCT series_id
    FROM RankedSeries
    WHERE rn <= :limitPerCreator
    """, nativeQuery = true)
    List<String> findTopSeriesFromFollowedCreators(
            @Param("accountId") String accountId,
            @Param("status") String status,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            @Param("limitPerCreator") int limitPerCreator
    );
}
