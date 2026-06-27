package com.talex.server.repositories;

import com.talex.server.entities.Episode;
import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.EpisodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, String> {
    Optional<Episode> findByEpisodeIdAndIsDeletedFalse(String episodeId);

    Optional<Episode> findByEpisodeIdAndCreatorIdAndIsDeletedFalse(String episodeId, String creatorId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Episode e where e.episodeId = :episodeId and e.isDeleted = false")
    Optional<Episode> lockByEpisodeIdAndIsDeletedFalse(@Param("episodeId") String episodeId);

    List<Episode> findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(String seasonId);

    List<Episode> findAllBySeason_SeasonIdAndStatusAndIsDeletedFalseOrderByEpisodeNumberAsc(
            String seasonId,
            EpisodeStatus status);

    List<Episode> findTop100ByScheduledPublishAtLessThanEqualAndStatusInAndIsDeletedFalseOrderByScheduledPublishAtAsc(
            LocalDateTime scheduledPublishAt,
            Collection<EpisodeStatus> statuses);

    @Query("""
            select count(e)
            from Episode e
            where e.season.series.seriesId = :seriesId
              and e.episodeId <> :episodeId
              and e.status = :status
              and e.isDeleted = false
            """)
    long countBySeriesIdExcludingEpisodeAndStatus(
            @Param("seriesId") String seriesId,
            @Param("episodeId") String episodeId,
            @Param("status") EpisodeStatus status);

    @Query("""
            select count(e)
            from Episode e
            where e.season.seasonId = :seasonId
              and e.episodeId <> :episodeId
              and e.status = :status
              and e.isDeleted = false
            """)
    long countBySeasonIdExcludingEpisodeAndStatus(
            @Param("seasonId") String seasonId,
            @Param("episodeId") String episodeId,
            @Param("status") EpisodeStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update episodes e
            set creator_id = sr.creator_id
            from seasons s
            join series sr on sr.series_id = s.series_id
            where e.season_id = s.season_id
              and e.creator_id is distinct from sr.creator_id
            """, nativeQuery = true)
    int synchronizeCreatorIdsFromSeries();
}
