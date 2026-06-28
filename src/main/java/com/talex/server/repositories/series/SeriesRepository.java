package com.talex.server.repositories.series;

import com.talex.server.entities.series.Series;
import com.talex.server.enums.SeriesStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, String> {
    Optional<Series> findBySeriesIdAndIsDeletedFalse(String seriesId);

    Page<Series> findAllByIsDeletedFalse(Pageable pageable);

    Page<Series> findAllByCreator_CreatorIdAndIsDeletedFalse(String creatorId, Pageable pageable);

    Page<Series> findAllByStatusAndIsDeletedFalse(
            SeriesStatus status,
            Pageable pageable);
}
