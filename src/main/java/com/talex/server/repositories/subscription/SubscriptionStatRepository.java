package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionStat;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SubscriptionStatRepository extends JpaRepository<SubscriptionStat, String> {

    // Kiểm tra AccountSub hợp lệ với accountId và timestamp (startTime)
    @Query("SELECT sub.accountSubscriptionId " +
            "FROM Account a " +
            "JOIN a.accountSubscriptions sub " +
            "WHERE a.accountId = :accountId " +
            "AND sub.isCancelled = false " +
            "AND sub.startTime <= :startTime " +
            "AND sub.endTime >= :startTime")
    Optional<String> findActiveAccountSubId(
            @Param("accountId") UUID accountId,
            @Param("startTime") LocalDateTime startTime
    );

    // Cộng views lên 1 nếu bản ghi tháng đó & creator đó đã tồn tại
    @Modifying
    @Query("UPDATE SubscriptionStat s " +
            "SET s.views = s.views + 1 " +
            "WHERE s.accountSubscription.accountSubscriptionId = :accountSubId " +
            "AND s.creatorId = :creatorId " +
            "AND s.monthYear = :monthYear")
    int incrementViews(
            @Param("accountSubId") String accountSubId,
            @Param("creatorId") String creatorId,
            @Param("monthYear") String monthYear
    );
}