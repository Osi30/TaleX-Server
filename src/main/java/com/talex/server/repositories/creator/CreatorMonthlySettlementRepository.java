package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.enums.transaction.SettlementStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreatorMonthlySettlementRepository extends
        JpaRepository<CreatorMonthlySettlement, String>,
        JpaSpecificationExecutor<CreatorMonthlySettlement> {
    List<CreatorMonthlySettlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);

    Optional<CreatorMonthlySettlement> findByCreatorMonthlySettlementId(String id);

    @Query("SELECT s FROM CreatorMonthlySettlement s " +
            "WHERE s.settlementMonth LIKE CONCAT(:year, '-%') " +
            "AND s.status IN :statuses " +
            "AND (:creatorId IS NULL OR s.creator.creatorId = :creatorId)")
    List<CreatorMonthlySettlement> findForTaxByYearAndCreator(
            @Param("year") String year,
            @Param("statuses") List<SettlementStatus> statuses,
            @Param("creatorId") String creatorId
    );

    @Query("SELECT s FROM CreatorMonthlySettlement s " +
            "WHERE (:yearMonth IS NULL OR s.settlementMonth = :yearMonth) " +
            "AND (:status IS NULL OR s.status = :status)")
    Page<CreatorMonthlySettlement> filterPitSettlements(
            @Param("yearMonth") String yearMonth,
            @Param("status") SettlementStatus status,
            Pageable pageable
    );
}