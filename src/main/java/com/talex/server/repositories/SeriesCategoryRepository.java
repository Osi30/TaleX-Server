package com.talex.server.repositories;

import com.talex.server.entities.SeriesCategory;
import com.talex.server.entities.SeriesCategoryId;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesCategoryRepository extends JpaRepository<SeriesCategory, SeriesCategoryId> {
    List<SeriesCategory> findBySeries_SeriesId(String seriesId);

    @EntityGraph(attributePaths = "category")
    List<SeriesCategory> findBySeries_SeriesIdAndIsDeletedFalse(String seriesId);

    @EntityGraph(attributePaths = "category")
    List<SeriesCategory> findBySeries_SeriesIdInAndIsDeletedFalse(Collection<String> seriesIds);

    Optional<SeriesCategory> findBySeries_SeriesIdAndCategory_CategoryIdAndIsDeletedFalse(
            String seriesId,
            String categoryId);
}
