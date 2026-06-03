package com.talex.server.repositories;

import com.talex.server.entities.Episode;
import com.talex.server.enums.EpisodeStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EpisodeRepository extends JpaRepository<Episode, String> {
    Optional<Episode> findByEpisodeIdAndIsDeletedFalse(String episodeId);

    List<Episode> findAllBySeason_SeasonIdAndIsDeletedFalseOrderByEpisodeNumberAsc(String seasonId);

    List<Episode> findAllBySeason_SeasonIdAndStatusAndIsDeletedFalseOrderByEpisodeNumberAsc(
            String seasonId,
            EpisodeStatus status);
}
