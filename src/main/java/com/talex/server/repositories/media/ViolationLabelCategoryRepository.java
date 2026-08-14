package com.talex.server.repositories.media;

import com.talex.server.entities.media.ViolationLabelCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViolationLabelCategoryRepository extends JpaRepository<ViolationLabelCategory, UUID> {

    List<ViolationLabelCategory> findAllByIsDeletedFalseOrderByNameAsc();

    Optional<ViolationLabelCategory> findByCategoryIdAndIsDeletedFalse(UUID categoryId);

    boolean existsByNameAndIsDeletedFalse(String name);
}
