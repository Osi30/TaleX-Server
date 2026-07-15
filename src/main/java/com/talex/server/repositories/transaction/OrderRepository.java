package com.talex.server.repositories.transaction;

import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.records.MonetizationData;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT o FROM Order o WHERE o.paymentCode = :paymentCode")
    Optional<Order> findWithLockByPaymentCode(@Param("paymentCode") String paymentCode);

    @Query("SELECT o.account.accountId as accountId, " +
            "SUM(o.totalAmount) as totalSpentAmount, " +
            "COUNT(CASE WHEN o.itemType = 'PREMIUM_SUBSCRIPTION' THEN 1 END) as premiumSubscriptionCount, " +
            "COUNT(CASE WHEN o.itemType = 'SINGLE_PURCHASE' THEN 1 END) as singlePurchaseCount, " +
            "COUNT(CASE WHEN o.itemType = 'INTERACTION_PUSH' THEN 1 END) as interactionPushCount, " +
            "MAX(o.createdAt) as lastPurchaseTime " +
            "FROM Order o " +
            "WHERE o.status = 'COMPLETED' " +
            "  AND o.createdAt > :startTime " +
            "  AND o.createdAt <= :endTime " +
            "GROUP BY o.account.accountId")
    List<MonetizationData> aggregateMonetizationStatsDelta(
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime);
}
