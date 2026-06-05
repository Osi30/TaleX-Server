package com.talex.server.repositories;

import com.talex.server.entities.SeriesTag;
import com.talex.server.entities.SeriesTagId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesTagRepository extends JpaRepository<SeriesTag, SeriesTagId> {
    List<SeriesTag> findBySeries_SeriesId(String seriesId);

    List<SeriesTag> findBySeries_SeriesIdAndIsDeletedFalse(String seriesId);

    Optional<SeriesTag> findBySeries_SeriesIdAndTag_TagIdAndIsDeletedFalse(
            String seriesId,
            String tagId);
}
