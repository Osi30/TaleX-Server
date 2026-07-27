package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorTier;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
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

    Optional<CreatorTier> findByIsDefaultTrueAndIsDeletedFalse();

    List<CreatorTier> findAllByIsDeletedFalseOrderByTierLevelAsc();

    @Modifying
    @Query("UPDATE CreatorTier ct SET ct.isDefault = false WHERE ct.creatorTierId <> :id AND ct.isDefault = true")
    void unsetOtherDefaults(@Param("id") String id);

    // Lấy ra Tier cao nhất đạt đủ điều kiện chỉ số
    @Query("""
        SELECT ct FROM CreatorTier ct
        WHERE ct.isDeleted = false
          AND ct.minFollowerRequired <= :followers
          AND ct.minViewsRequired <= :views
          AND ct.minWatchTimeRequired <= :watchTime
        ORDER BY ct.tierLevel DESC
    """)
    List<CreatorTier> findEligibleTiers(
            @Param("followers") Long followers,
            @Param("views") Long views,
            @Param("watchTime") Double watchTime,
            Pageable pageable
    );

    default Optional<CreatorTier> findCurrentEligibleTier(Long followers, Long views, Double watchTime) {
        List<CreatorTier> tiers = findEligibleTiers(followers, views, watchTime, PageRequest.of(0, 1));
        return tiers.isEmpty() ? Optional.empty() : Optional.of(tiers.getFirst());
    }

    // Lấy ra Tier tiếp theo lớn hơn level hiện tại
    Optional<CreatorTier> findFirstByTierLevelGreaterThanAndIsDeletedFalseOrderByTierLevelAsc(Integer currentTierLevel);
}
