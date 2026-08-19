package com.talex.server.repositories.subscription;

import com.talex.server.dtos.subscription.dtos.SubscriptionStatRawData;
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

    @Query("SELECT new com.talex.server.dtos.subscription.dtos.SubscriptionStatRawData(" +
            "s.accountSubscription.account.accountId, " +
            "sub.accountSubscriptionId, " +
            "sub.startTime, " +
            "sub.endTime, " +
            "o.totalAmount, " +
            "o.vatAmount, " +
            "s.creatorId, " +
            "s.episodeId, " +
            "SUM(s.views)) " +
            "FROM SubscriptionStat s " +
            "JOIN s.accountSubscription sub " +
            "LEFT JOIN Order o ON sub.orderId = o.orderId " +
            "WHERE sub.endTime >= :startOfMonth " +
            "  AND sub.endTime <= :endOfMonth " +
            "GROUP BY s.accountSubscription.account.accountId, " +
            "         sub.accountSubscriptionId, " +
            "         sub.startTime, " +
            "         sub.endTime, " +
            "         o.totalAmount, " +
            "         o.vatAmount, " +
            "         s.creatorId, " +
            "         s.episodeId")
    List<SubscriptionStatRawData> findGroupedStatsWithOrderDetailsByMonthYear(
            @Param("startOfMonth") LocalDateTime startOfMonth,
            @Param("endOfMonth") LocalDateTime endOfMonth
    );

    @Query("SELECT st.id AS id, " +
            "st.monthYear AS monthYear, " +
            "st.creatorId AS creatorId, " +
            "acc.username AS creatorUsername, " +
            "acc.avatarUrl AS creatorAvatarUrl, " +
            "st.episodeId AS episodeId, " +
            "ep.title AS episodeTitle, " +
            "ep.episodeNumber AS episodeNumber, " +
            "ser.seriesId AS seriesId, " +
            "ser.title AS seriesTitle, " +
            "ser.coverUrl AS coverUrl, " +
            "ser.bannerUrl AS bannerUrl, " +
            "st.views AS views, " +
            "sub.accountSubscriptionId AS accountSubscriptionId " +
            "FROM SubscriptionStat st " +
            "JOIN st.accountSubscription sub " +
            "LEFT JOIN Episode ep ON st.episodeId = ep.episodeId " +
            "LEFT JOIN ep.season sea " +
            "LEFT JOIN sea.series ser " +
            "LEFT JOIN Creator c ON st.creatorId = c.creatorId " +
            "LEFT JOIN c.account acc " +
            "WHERE sub.accountSubscriptionId = :accountSubscriptionId")
    Page<Object[]> findDetailedStatsByAccountSubscriptionId(
            @Param("accountSubscriptionId") String accountSubscriptionId,
            Pageable pageable
    );
}