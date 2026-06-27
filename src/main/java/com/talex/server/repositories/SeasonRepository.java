package com.talex.server.repositories;

import com.talex.server.entities.Season;
import com.talex.server.enums.SeasonStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
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

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update seasons s
            set creator_id = sr.creator_id
            from series sr
            where s.series_id = sr.series_id
              and s.creator_id is distinct from sr.creator_id
            """, nativeQuery = true)
    int synchronizeCreatorIdsFromSeries();
}
