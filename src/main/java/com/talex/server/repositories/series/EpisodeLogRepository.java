package com.talex.server.repositories.series;

import com.talex.server.entities.series.EpisodeLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EpisodeLogRepository extends JpaRepository<EpisodeLog, String> {

    List<EpisodeLog> findByEpisode_EpisodeIdAndHourBucketBetweenOrderByHourBucketAsc(
            String episodeId,
            LocalDateTime from,
            LocalDateTime to
    );
}