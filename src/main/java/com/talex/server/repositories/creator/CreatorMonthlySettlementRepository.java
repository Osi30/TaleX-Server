package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.enums.transaction.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CreatorMonthlySettlementRepository extends
        JpaRepository<CreatorMonthlySettlement, String>,
        JpaSpecificationExecutor<CreatorMonthlySettlement> {
    List<CreatorMonthlySettlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);

    Optional<CreatorMonthlySettlement> findByCreatorMonthlySettlementId(String id);
}