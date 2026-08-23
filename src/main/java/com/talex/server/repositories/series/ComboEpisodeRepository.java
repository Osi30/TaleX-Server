package com.talex.server.repositories.series;

import com.talex.server.entities.series.ComboEpisode;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ComboEpisodeRepository extends JpaRepository<ComboEpisode, String> {
    List<ComboEpisode> findByCreatorIdAndIsDeletedFalse(String creatorId);
    List<ComboEpisode> findByIsDeletedFalse();

    @Query("SELECT c.creatorId " +
            "FROM ComboEpisode c " +
            "WHERE c.comboId = :comboId")
    Optional<String> findCreatorIdByComboId(@Param("comboId") String comboId);
}
