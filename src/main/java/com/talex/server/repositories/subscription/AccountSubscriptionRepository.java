package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.AccountSubscription;
import com.talex.server.records.CreatorPoolData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface AccountSubscriptionRepository extends JpaRepository<AccountSubscription, String> {
    Page<AccountSubscription> findAll(Specification<AccountSubscription> example, Pageable pageable);

    Optional<AccountSubscription> findFirstByAccount_AccountIdAndIsCancelledFalseAndStartTimeLessThanEqualAndEndTimeGreaterThanEqualOrderByEndTimeDesc(
            UUID accountId, LocalDateTime startTimeThreshold, LocalDateTime endTimeThreshold);

    @Query("SELECT sub " +
            "FROM Account a " +
            "JOIN a.accountSubscriptions sub " +
            "WHERE a.accountId = :accountId " +
            "AND sub.isCancelled = false " +
            "AND sub.startTime <= :now " +
            "AND sub.endTime >= :now")
    Optional<AccountSubscription> findActiveAccountSubId(
            @Param("accountId") UUID accountId,
            @Param("now") LocalDateTime now
    );

    // Lấy tổng tiền pool được tính trong tháng để chia sẻ cho các nhà sáng tạo
    @Query("SELECT new com.talex.server.records.CreatorPoolData(" +
            "SUM(o.totalAmount), " +
            "SUM(o.vatAmount), " +
            "SUM(o.fiatAmount), " +
            "COUNT(sub)) " +
            "FROM AccountSubscription sub " +
            "JOIN Order o ON sub.orderId = o.orderId " +
            "WHERE sub.endTime >= :startOfMonth " +
            "AND sub.endTime <= :endOfMonth " +
            "AND sub.subscription.subscriptionId = :subscriptionId")
    CreatorPoolData calculateCreatorPoolSummary(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth,
            @Param("subscriptionId") String subscriptionId
    );

    @Query("SELECT sub.accountSubscriptionId AS accountSubscriptionId, " +
            "sub.orderId AS orderId, " +
            "o.totalAmount AS totalAmount, " +
            "o.vatAmount AS vatAmount, " +
            "sub.startTime AS startTime, " +
            "sub.endTime AS endTime, " +
            "sub.totalViews AS totalViews, " +
            "sub.account.accountId AS accountId, " +
            "sub.account.email AS email " +
            "FROM AccountSubscription sub " +
            "JOIN Order o ON sub.orderId = o.orderId " +
            "WHERE sub.endTime >= :startOfMonth " +
            "AND sub.endTime <= :endOfMonth " +
            "AND (:subscriptionId IS NULL OR :subscriptionId = '' OR sub.subscription.subscriptionId = :subscriptionId)")
    Page<Object[]> findCreatorPoolDetailsRaw(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth,
            @Param("subscriptionId") String subscriptionId,
            Pageable pageable
    );
}
