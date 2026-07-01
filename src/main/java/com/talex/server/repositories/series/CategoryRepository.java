package com.talex.server.repositories.series;

import com.talex.server.entities.series.Category;
import com.talex.server.enums.series.CategoryStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

@Repository
public interface CategoryRepository extends JpaRepository<Category, String> {
    Optional<Category> findByCategoryIdAndIsDeletedFalse(String categoryId);

    List<Category> findAllByCategoryIdInAndIsDeletedFalse(Collection<String> categoryIds);

    Optional<Category> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    Page<Category> findAllByIsDeletedFalse(Pageable pageable);

    Page<Category> findAllByStatusAndIsDeletedFalse(CategoryStatus status, Pageable pageable);
}
