package com.talex.server.repositories;

import com.talex.server.entities.Series;
import com.talex.server.enums.SeriesStatus;
import com.talex.server.enums.Visibility;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SeriesRepository extends JpaRepository<Series, String> {
    Optional<Series> findBySeriesIdAndIsDeletedFalse(String seriesId);

    Page<Series> findAllByIsDeletedFalse(Pageable pageable);

    Page<Series> findAllByCreatorIdAndIsDeletedFalse(String creatorId, Pageable pageable);

    Page<Series> findAllByVisibilityAndStatusAndIsDeletedFalse(
            Visibility visibility,
            SeriesStatus status,
            Pageable pageable);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(value = """
            update series s
            set creator_id = c.creator_id
            from creator c
            where (s.creator_id = cast(c.account_id as varchar)
                   or (s.creator_id is null and s.created_by = cast(c.account_id as varchar)))
              and s.creator_id is distinct from c.creator_id
            """, nativeQuery = true)
    int migrateAccountIdsToCreatorIds();
}
