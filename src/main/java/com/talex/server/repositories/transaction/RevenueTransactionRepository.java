package com.talex.server.repositories.transaction;

import com.talex.server.entities.creator.RevenueTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface RevenueTransactionRepository extends JpaRepository<RevenueTransaction, String> {
    @Query("SELECT r FROM RevenueTransaction r " +
            "JOIN FETCH r.creator c " +
            "LEFT JOIN FETCH c.account a " +
            "WHERE r.creatorMonthlySettlement IS NULL " +
            "AND r.monthYear <= :targetMonthYear")
    List<RevenueTransaction> findUnsettledTransactionsUpToMonth(
            @Param("targetMonthYear") LocalDate targetMonthYear
    );
}