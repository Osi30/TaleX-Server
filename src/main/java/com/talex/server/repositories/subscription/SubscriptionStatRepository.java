package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionStat;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
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
            "  AND s.creatorId = :creatorId " +
            "  AND s.episodeId = :episodeId " +
            "  AND s.monthYear = :monthYear")
    int incrementViews(
            @Param("accountSubId") String accountSubId,
            @Param("creatorId") String creatorId,
            @Param("episodeId") String episodeId,
            @Param("monthYear") String monthYear
    );

    @Query("SELECT s.accountSubscription.account.accountId, s.creatorId, s.episodeId, SUM(s.views) " +
            "FROM SubscriptionStat s " +
            "JOIN s.accountSubscription sub " +
            "WHERE sub.subscription.subscriptionId = :subscriptionId " +
            "  AND sub.endTime >= :startOfMonth " +
            "  AND sub.endTime <= :endOfMonth " +
            "GROUP BY s.accountSubscription.account.accountId, s.creatorId, s.episodeId")
    List<Object[]> findGroupedStatsByMonthYear(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth,
            @Param("subscriptionId") String subscriptionId
    );

    @Query("SELECT s.id, " +
            "s.monthYear, " +
            "s.creatorId, " +
            "a.email, " +
            "s.episodeId, " +
            "e.episodeNumber, " +
            "ser.seriesId, " +
            "ser.title, " +
            "s.views " +
            "FROM SubscriptionStat s " +
            "LEFT JOIN Creator c ON s.creatorId = c.creatorId " +
            "LEFT JOIN c.account a " +
            "LEFT JOIN Episode e ON s.episodeId = e.episodeId " +
            "LEFT JOIN e.season se " +
            "LEFT JOIN se.series ser " +
            "WHERE s.accountSubscription.accountSubscriptionId = :accountSubscriptionId")
    Page<Object[]> findStatsDetailsByAccountSubId(
            @Param("accountSubscriptionId") String accountSubscriptionId,
            Pageable pageable
    );
}