package com.talex.server.repositories.media;

import com.talex.server.entities.media.ViolationLabelTranslation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ViolationLabelTranslationRepository extends JpaRepository<ViolationLabelTranslation, UUID> {

    List<ViolationLabelTranslation> findAllByIsDeletedFalseOrderByCategoryAscAwsLabelAsc();

    Optional<ViolationLabelTranslation> findByTranslationIdAndIsDeletedFalse(UUID translationId);

    Optional<ViolationLabelTranslation> findByAwsLabelAndIsDeletedFalse(String awsLabel);

    boolean existsByAwsLabelAndIsDeletedFalse(String awsLabel);
}
