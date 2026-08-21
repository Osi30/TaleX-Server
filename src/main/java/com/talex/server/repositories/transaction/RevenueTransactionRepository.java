package com.talex.server.repositories.transaction;

import com.talex.server.entities.creator.RevenueTransaction;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
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

    /**
     * Tính tổng số tiền chưa quyết toán của 1 episode
     */
    @Query(value = """
        SELECT COALESCE(SUM(rt.amount), 0)
        FROM revenue_transaction rt
        INNER JOIN orders o ON rt.reference_id = o.order_id
        WHERE rt.creator_monthly_settlement_id IS NULL
          AND rt.reference_type = 'ORDER'
          AND rt.change_type = 'CONTENT_SHARE'
          AND o.item_id = :episodeId
        """, nativeQuery = true)
    BigDecimal calculateUnsettledRevenueByEpisodeId(@Param("episodeId") String episodeId);
}