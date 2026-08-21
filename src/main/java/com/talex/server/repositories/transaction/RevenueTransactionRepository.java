package com.talex.server.repositories.transaction;

import com.talex.server.entities.creator.RevenueTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
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

    // 1. Phân trang tất cả hoặc lọc theo creatorId
    Page<RevenueTransaction> findAllByOrderByCreatedAtDesc(Pageable pageable);

    Page<RevenueTransaction> findByCreator_CreatorIdOrderByCreatedAtDesc(String creatorId, Pageable pageable);

    // 2. Query lọc theo khoảng thời gian created_at
    List<RevenueTransaction> findByCreatedAtBetweenOrderByCreatedAtAsc(
            LocalDateTime startDate, LocalDateTime endDate
    );

    List<RevenueTransaction> findByCreator_CreatorIdAndCreatedAtBetweenOrderByCreatedAtAsc(
            String creatorId, LocalDateTime startDate, LocalDateTime endDate
    );
}