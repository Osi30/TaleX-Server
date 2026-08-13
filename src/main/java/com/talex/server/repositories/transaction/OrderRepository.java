package com.talex.server.repositories.transaction;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.records.OrderStatisticData;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface OrderRepository extends JpaRepository<Order, String> {

    Optional<Order> findByPaymentCode(String paymentCode);

    List<Order> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            OrderStatus status, LocalDateTime now);

    @Query(value = "SELECT nextval('payment_code_seq')", nativeQuery = true)
    long nextPaymentCodeSequence();

    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.account.accountId = :accountId")
    Optional<Order> findByOrderIdAndAccountId(@Param("orderId") String orderId, @Param("accountId") java.util.UUID accountId);

    Optional<Order> findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
            java.util.UUID accountId, String itemType, String itemId, OrderStatus status);

    Page<Order> findByAccount_AccountIdAndItemTypeInOrderByCreatedAtDesc(
            UUID accountId, List<String> itemTypes, Pageable pageable);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.paymentCode = :paymentCode")
    Optional<Order> findWithLockByPaymentCode(@Param("paymentCode") String paymentCode);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId")
    Optional<Order> findWithLockByOrderId(@Param("orderId") String orderId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.account.accountId = :accountId")
    Optional<Order> findWithLockByOrderIdAndAccountId(@Param("orderId") String orderId, @Param("accountId") UUID accountId);

    /**
     * Lấy tổng quan thống kê (GMV, Doanh thu thuần, VAT, Coin) trong khoảng thời gian
     */
    @Query(value = """
        SELECT
            TO_CHAR(NOW(), 'YYYY') AS period,
            COALESCE(SUM(o.total_amount), 0) AS gmv,
            COALESCE(SUM(o.fiat_amount - o.vat_amount), 0) AS netRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.coin_amount), 0) AS totalCoin
        FROM orders o
        WHERE o.status = :status
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        """, nativeQuery = true)
    OrderStatisticData getOverviewStatistic(
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Thống kê gom nhóm theo Ngày/Tháng (truyền pattern 'YYYY-MM-DD' hoặc 'YYYY-MM')
     */
    @Query(value = """
        SELECT
            TO_CHAR(o.created_at, :dateFormatPattern) AS period,
            COALESCE(SUM(o.total_amount), 0) AS gmv,
            COALESCE(SUM(o.fiat_amount - o.vat_amount), 0) AS netRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.coin_amount), 0) AS totalCoin
        FROM orders o
        WHERE o.status = :status
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        GROUP BY 1
        ORDER BY period ASC
        """, nativeQuery = true)
    List<OrderStatisticData> getGroupedStatistics(
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("dateFormatPattern") String dateFormatPattern
    );

    @Query("SELECT SUM(o.vatAmount) FROM Order o " +
            "WHERE o.status = :status AND o.itemType IN :itemTypes " +
            "AND o.createdAt BETWEEN :startDate AND :endDate")
    BigDecimal sumVatByItemTypesAndDateRange(
            @Param("status") OrderStatus status,
            @Param("itemTypes") List<String> itemTypes,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate
    );

    @Query("SELECT o FROM Order o " +
            "WHERE (:status IS NULL OR o.status = :status) " +
            "AND (:itemType IS NULL OR o.itemType = :itemType) " +
            "AND o.createdAt BETWEEN :startDate AND :endDate " +
            "ORDER BY o.createdAt ASC")
    Page<Order> filterVatOrders(
            @Param("status") OrderStatus status,
            @Param("itemType") String itemType,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate,
            Pageable pageable
    );
}
