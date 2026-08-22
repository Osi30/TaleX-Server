package com.talex.server.repositories.series;

import com.talex.server.entities.series.Episode;
import com.talex.server.enums.series.EpisodeStatus;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, String> {
    @EntityGraph(attributePaths = {"season", "season.series"})
    Optional<Episode> findByEpisodeIdAndIsDeletedFalse(String episodeId);

    @EntityGraph(attributePaths = {"season", "season.series"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select e from Episode e where e.episodeId = :episodeId and e.isDeleted = false")
    Optional<Episode> lockByEpisodeIdAndIsDeletedFalse(@Param("episodeId") String episodeId);

    @Query("""
            SELECT e.episodeId 
            FROM Episode e 
            WHERE e.season.series.seriesId = :seriesId 
              AND e.status <> com.talex.server.enums.series.EpisodeStatus.FORCE_HIDDEN 
            """)
    List<String> findEpisodeIdsBySeriesId(@Param("seriesId") String seriesId);

    List<Episode> findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(String seasonId);

    List<Episode> findAllBySeason_SeasonIdAndStatusInAndIsDeletedFalseOrderByEpisodeNumberAsc(
            String seasonId,
            Collection<EpisodeStatus> statuses);

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

    @Query("select coalesce(max(e.episodeNumber), 0) from Episode e where e.season.seasonId = :seasonId and e.isDeleted = false")
    Integer findMaxEpisodeNumberBySeasonId(@Param("seasonId") String seasonId);

    @Query("SELECT e.season.series.seriesId FROM Episode e WHERE e.episodeId = :episodeId")
    Optional<String> findSeriesIdByEpisodeId(@Param("episodeId") String episodeId);

    @Query("SELECT e.creatorId " +
            "FROM Episode e " +
            "WHERE e.episodeId = :episodeId")
    Optional<String> getCreatorIdByEpisodeId(@Param("episodeId") String episodeId);
}
