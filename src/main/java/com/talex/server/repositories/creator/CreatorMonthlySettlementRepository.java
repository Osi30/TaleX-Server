package com.talex.server.repositories.creator;

import com.talex.server.entities.creator.CreatorMonthlySettlement;
import com.talex.server.enums.transaction.SettlementStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CreatorMonthlySettlementRepository extends JpaRepository<CreatorMonthlySettlement, String> {
    List<CreatorMonthlySettlement> findBySettlementMonth(String settlementMonth);

    List<CreatorMonthlySettlement> findBySettlementMonthAndStatus(String settlementMonth, SettlementStatus status);
}