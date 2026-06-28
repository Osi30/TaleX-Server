package com.talex.server.repositories.series;

import com.talex.server.entities.series.SeriesTag;
import com.talex.server.entities.series.SeriesTagId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesTagRepository extends JpaRepository<SeriesTag, SeriesTagId> {
    List<SeriesTag> findBySeries_SeriesId(String seriesId);

    @EntityGraph(attributePaths = "tag")
    List<SeriesTag> findBySeries_SeriesIdAndIsDeletedFalse(String seriesId);

    @EntityGraph(attributePaths = "tag")
    List<SeriesTag> findBySeries_SeriesIdInAndIsDeletedFalse(Collection<String> seriesIds);

    Optional<SeriesTag> findBySeries_SeriesIdAndTag_TagIdAndIsDeletedFalse(
            String seriesId,
            String tagId);
}
