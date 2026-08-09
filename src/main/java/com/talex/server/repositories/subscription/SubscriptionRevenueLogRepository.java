package com.talex.server.repositories.subscription;

import com.talex.server.entities.subscription.SubscriptionRevenueLog;
import com.talex.server.records.CreatorRevenueData;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
}