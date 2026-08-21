package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionRevenueLog;
import com.talex.server.records.CreatorRevenueData;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;

@Repository
public interface SubscriptionRevenueLogRepository extends JpaRepository<SubscriptionRevenueLog, String> {
    @Query("SELECT l.creatorId AS creatorId, SUM(l.revenue) AS totalRevenue, l.subscriptionResult.id AS subscriptionResultId " +
            "FROM SubscriptionRevenueLog l " +
            "WHERE l.monthYear = :monthYear " +
            "GROUP BY l.creatorId, l.subscriptionResult.id")
    List<CreatorRevenueData> findAggregatedRevenueByMonthYear(
            @Param("monthYear") String monthYear
    );

    @Query("SELECT log.id AS id, " +
            "log.monthYear AS monthYear, " +
            "log.revenue AS revenue, " +
            "log.creatorId AS creatorId, " +
            "acc.username AS username, " +
            "acc.avatarUrl AS avatarUrl, " +
            "log.episodeId AS episodeId, " +
            "ep.title AS episodeTitle, " +
            "ep.episodeNumber AS episodeNumber, " +
            "ser.seriesId AS seriesId, " +
            "ser.title AS seriesTitle, " +
            "ser.coverUrl AS coverUrl, " +
            "ser.bannerUrl AS bannerUrl, " +
            "res.id AS subscriptionResultId " +
            "FROM SubscriptionRevenueLog log " +
            "JOIN log.subscriptionResult res " +
            "LEFT JOIN Episode ep ON log.episodeId = ep.episodeId " +
            "LEFT JOIN ep.season sea " +
            "LEFT JOIN sea.series ser " +
            "LEFT JOIN Creator c ON log.creatorId = c.creatorId " +
            "LEFT JOIN c.account acc " +
            "WHERE res.id = :subscriptionResultId")
    Page<Object[]> findDetailedLogsBySubscriptionResultId(
            @Param("subscriptionResultId") String subscriptionResultId,
            Pageable pageable
    );

    /**
     * Tính tổng doanh thu Subscription chưa quyết toán của 1 episode
     */
    @Query(value = """
        SELECT COALESCE(SUM(srl.revenue), 0)
        FROM subscription_revenue_logs srl
        INNER JOIN subscription_results sr ON srl.subscription_result_id = sr.id
        WHERE srl.episode_id = :episodeId
          AND EXISTS (
              SELECT 1 
              FROM revenue_transaction rt
              WHERE rt.reference_id = sr.id
                AND rt.creator_id = srl.creator_id
                AND rt.reference_type = 'PREMIUM_RESULT'
                AND rt.change_type = 'PREMIUM_SHARE'
                AND rt.creator_monthly_settlement_id IS NULL
          )
        """, nativeQuery = true)
    BigDecimal calculateUnsettledSubscriptionRevenueByEpisodeId(@Param("episodeId") String episodeId);
}