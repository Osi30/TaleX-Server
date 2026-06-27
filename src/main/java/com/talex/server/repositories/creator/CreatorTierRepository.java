package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorTier;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreatorTierRepository
        extends JpaRepository<CreatorTier, String>, JpaSpecificationExecutor<CreatorTier> {

    Optional<CreatorTier> findByCreatorTierIdAndIsDeletedFalse(String id);

    boolean existsByTierLevelAndIsDeletedFalse(Integer tierLevel);

    boolean existsByTierLevelAndCreatorTierIdNotAndIsDeletedFalse(Integer tierLevel, String creatorTierId);

    List<CreatorTier> findAllByIsDeletedFalseOrderByTierLevelAsc();

    @Modifying
    @Query("UPDATE CreatorTier ct SET ct.isDefault = false WHERE ct.creatorTierId <> :id AND ct.isDefault = true")
    void unsetOtherDefaults(@Param("id") String id);
}
