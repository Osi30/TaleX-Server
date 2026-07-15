package com.talex.server.repositories.series;

import com.talex.server.entities.series.SeriesLog;
import com.talex.server.records.SeriesLogData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SeriesLogRepository extends JpaRepository<SeriesLog, String> {

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
}