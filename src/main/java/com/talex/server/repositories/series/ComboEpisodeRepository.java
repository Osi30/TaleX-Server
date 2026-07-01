package com.talex.server.repositories.series;

import com.talex.server.entities.series.ComboEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ComboEpisodeRepository extends JpaRepository<ComboEpisode, String> {
    List<ComboEpisode> findByCreatorIdAndIsDeletedFalse(String creatorId);
    List<ComboEpisode> findByIsDeletedFalse();
}
