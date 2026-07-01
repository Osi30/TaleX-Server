package com.talex.server.repositories.series;

import com.talex.server.entities.series.EpisodeUnlockedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface EpisodeUnlockedContentRepository extends JpaRepository<EpisodeUnlockedContent, UUID> {
    List<EpisodeUnlockedContent> findByAccount_AccountId(UUID accountId);
    boolean existsByAccount_AccountIdAndEpisode_EpisodeId(UUID accountId, String episodeId);
}
