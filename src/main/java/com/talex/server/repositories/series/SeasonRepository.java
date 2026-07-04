package com.talex.server.repositories.series;

import com.talex.server.entities.series.Season;
import com.talex.server.enums.series.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeasonRepository extends JpaRepository<Season, String> {
    Optional<Season> findBySeasonIdAndIsDeletedFalse(String seasonId);

    Optional<Season> findBySeasonIdAndCreatorIdAndIsDeletedFalse(String seasonId, String creatorId);

    List<Season> findAllBySeries_SeriesIdAndIsDeletedFalseOrderBySeasonNumberAsc(String seriesId);

    List<Season> findAllBySeries_SeriesIdAndStatusAndIsDeletedFalseOrderBySeasonNumberAsc(
            String seriesId,
            SeasonStatus status);

    @Query("select coalesce(max(s.seasonNumber), 0) from Season s where s.series.seriesId = :seriesId and s.isDeleted = false")
    Integer findMaxSeasonNumberBySeriesId(@Param("seriesId") String seriesId);
}
