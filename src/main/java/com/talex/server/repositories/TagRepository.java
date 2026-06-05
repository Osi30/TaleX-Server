package com.talex.server.repositories;

import com.talex.server.entities.Tag;
import com.talex.server.enums.TagStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TagRepository extends JpaRepository<Tag, String> {
    Optional<Tag> findByTagIdAndIsDeletedFalse(String tagId);

    Optional<Tag> findBySlugAndIsDeletedFalse(String slug);

    boolean existsBySlugAndIsDeletedFalse(String slug);

    Page<Tag> findAllByIsDeletedFalse(Pageable pageable);

    Page<Tag> findAllByStatusAndIsDeletedFalse(TagStatus status, Pageable pageable);
}
