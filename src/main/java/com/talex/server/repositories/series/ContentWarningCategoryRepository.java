package com.talex.server.repositories.series;

import com.talex.server.entities.series.ContentWarningCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ContentWarningCategoryRepository extends JpaRepository<ContentWarningCategory, UUID> {

    List<ContentWarningCategory> findAllByIsDeletedFalseAndIsActiveTrueOrderByLabelAsc();

    List<ContentWarningCategory> findAllByIsDeletedFalseOrderByLabelAsc();

    Optional<ContentWarningCategory> findByCategoryIdAndIsDeletedFalse(UUID categoryId);

    boolean existsByCodeAndIsDeletedFalse(String code);

    List<ContentWarningCategory> findAllByCodeInAndIsDeletedFalse(List<String> codes);
}
