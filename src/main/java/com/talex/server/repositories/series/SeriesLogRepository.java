package com.talex.server.repositories.series;

import com.talex.server.entities.series.SeriesLog;
import com.talex.server.records.SeriesLogData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

@Repository
public interface SeriesLogRepository extends JpaRepository<SeriesLog, String> {
    List<SeriesLog> findBySeriesSeriesIdAndHourBucketBetweenOrderByHourBucketAsc(
            String seriesId,
            LocalDateTime start,
            LocalDateTime end
    );

    // Luồng Cumulative: gom nhóm trong khoảng (start, end)
    @Query("SELECT new com.talex.server.records.SeriesLogData(" +
            "  sl.series.seriesId, " +
            "  SUM(sl.analyticData.views), " +
            "  SUM(sl.analyticData.likes), " +
            "  SUM(sl.analyticData.bookmarks), " +
            "  SUM(sl.analyticData.shares), " +
            "  SUM(sl.analyticData.comments), " +
            "  SUM(sl.analyticData.watchTime)" +
            ") " +
            "FROM SeriesLog sl " +
            "WHERE sl.hourBucket > :start AND sl.hourBucket < :end " +
            "GROUP BY sl.series.seriesId")
    List<SeriesLogData> aggregateByHourBucketBetweenExclusive(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    // Luồng 24h & 7d: gom nhóm trong khoảng [start, end)
    @Query("SELECT new com.talex.server.records.SeriesLogData(" +
            "  sl.series.seriesId, " +
            "  SUM(sl.analyticData.views), " +
            "  SUM(sl.analyticData.likes), " +
            "  SUM(sl.analyticData.bookmarks), " +
            "  SUM(sl.analyticData.shares), " +
            "  SUM(sl.analyticData.comments), " +
            "  SUM(sl.analyticData.watchTime)" +
            ") " +
            "FROM SeriesLog sl " +
            "WHERE sl.hourBucket >= :start AND sl.hourBucket < :end " +
            "GROUP BY sl.series.seriesId")
    List<SeriesLogData> aggregateByHourBucketBetweenInclusiveStart(
            @Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

    @Query(value = """
    WITH RankedLogs AS (
        SELECT
            sl.series_id,
            sl.hour_bucket,
            sl.watch_time,
            sl.likes,
            sl.views,
            -- Đánh số thứ tự cho từng series, log ở khung giờ MỚI NHẤT sẽ có rn = 1
            ROW_NUMBER() OVER (
                PARTITION BY sl.series_id 
                ORDER BY sl.hour_bucket DESC
            ) AS rn
        FROM series_log sl
        JOIN series s ON sl.series_id = s.series_id
        WHERE s.is_deleted = false
          AND s.status = :status
          AND sl.hour_bucket < :beforeHour
          AND (:isBlacklistEmpty = true OR sl.series_id NOT IN (:blacklist))
    )
    -- Chỉ giữ lại log mới nhất (rn = 1) của từng series => Đảm bảo distinct 100%
    SELECT series_id
    FROM RankedLogs
    WHERE rn = 1
    ORDER BY 
        hour_bucket DESC,
        watch_time DESC,
        likes DESC,
        views DESC
    LIMIT :limit
""", nativeQuery = true)
    List<String> findCandidateTrendingSeriesIds(
            @Param("status") String status,
            @Param("beforeHour") LocalDateTime beforeHour,
            @Param("blacklist") Collection<String> blacklist,
            @Param("isBlacklistEmpty") boolean isBlacklistEmpty,
            @Param("limit") int limit
    );
}