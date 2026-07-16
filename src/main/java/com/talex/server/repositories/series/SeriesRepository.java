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
}
