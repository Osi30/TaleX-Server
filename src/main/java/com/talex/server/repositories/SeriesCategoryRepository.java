package com.talex.server.repositories;

import com.talex.server.entities.SeriesCategory;
import com.talex.server.entities.SeriesCategoryId;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SeriesCategoryRepository extends JpaRepository<SeriesCategory, SeriesCategoryId> {
    List<SeriesCategory> findBySeries_SeriesId(String seriesId);

    List<SeriesCategory> findBySeries_SeriesIdAndIsDeletedFalse(String seriesId);

    Optional<SeriesCategory> findBySeries_SeriesIdAndCategory_CategoryIdAndIsDeletedFalse(
            String seriesId,
            String categoryId);
}
