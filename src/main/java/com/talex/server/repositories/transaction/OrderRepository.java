package com.talex.server.repositories.transaction;

import com.talex.server.dtos.statistics.campaign.CampaignStatisticData;
import com.talex.server.dtos.statistics.content.ContentRevenueStatisticData;
import com.talex.server.dtos.statistics.subscription.SubscriptionStatisticData;
import com.talex.server.entities.transaction.Order;
import com.talex.server.enums.transaction.OrderStatus;
import com.talex.server.records.OrderDetailStatisticProjection;
import com.talex.server.records.OrderStatisticData;
import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
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
public interface OrderRepository extends JpaRepository<Order, String>, JpaSpecificationExecutor<Order> {

    List<Order> findTop100ByStatusAndExpiresAtLessThanEqualOrderByExpiresAtAsc(
            OrderStatus status, LocalDateTime now);

    @Query(value = "SELECT nextval('payment_code_seq')", nativeQuery = true)
    long nextPaymentCodeSequence();

    @Query("SELECT o FROM Order o WHERE o.orderId = :orderId AND o.account.accountId = :accountId")
    Optional<Order> findByOrderIdAndAccountId(@Param("orderId") String orderId, @Param("accountId") UUID accountId);

    Optional<Order> findFirstByAccount_AccountIdAndItemTypeAndItemIdAndStatusOrderByCreatedAtDesc(
            UUID accountId, String itemType, String itemId, OrderStatus status);

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
            COALESCE(SUM(o.fiat_amount + o.campaign_wallet_amount - o.vat_amount), 0) AS netRevenue,
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
            COALESCE(SUM(o.fiat_amount + o.campaign_wallet_amount - o.vat_amount), 0) AS netRevenue,
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

    /** Đếm số đơn theo từng status trong khoảng thời gian — dùng cho AdminOrderStatsDto. */
    @Query("SELECT o.status, COUNT(o) FROM Order o " +
            "WHERE o.createdAt BETWEEN :from AND :to " +
            "GROUP BY o.status")
    List<Object[]> countByStatusGrouped(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    /** Doanh thu đơn COMPLETED gom nhóm theo itemType trong khoảng thời gian. */
    @Query("SELECT o.itemType, SUM(o.totalAmount), COUNT(o) FROM Order o " +
            "WHERE o.status = :status AND o.createdAt BETWEEN :from AND :to " +
            "GROUP BY o.itemType")
    List<Object[]> sumRevenueByItemTypeGrouped(
            @Param("status") OrderStatus status,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    /**
     * Lấy tổng quan doanh thu Campaign (COMPLETED)
     */
    @Query(value = """
        SELECT
            'OVERVIEW' AS period,
            COALESCE(SUM(o.total_amount), 0) AS grossRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.total_amount - COALESCE(o.vat_amount, 0)), 0) AS netRevenue
        FROM orders o
        WHERE o.status = :status
          AND o.item_type = :itemType
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        """, nativeQuery = true)
    CampaignStatisticData getCampaignOverviewStatistic(
            @Param("status") String status,
            @Param("itemType") String itemType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy danh sách chi tiết doanh thu Campaign gom nhóm động theo chuỗi thời gian (COMPLETED)
     */
    @Query(value = """
        SELECT
            TO_CHAR(o.created_at, :dateFormatPattern) AS period,
            COALESCE(SUM(o.total_amount), 0) AS grossRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.total_amount - COALESCE(o.vat_amount, 0)), 0) AS netRevenue
        FROM orders o
        WHERE o.status = :status
          AND o.item_type = :itemType
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        GROUP BY 1
        ORDER BY period ASC
        """, nativeQuery = true)
    List<CampaignStatisticData> getCampaignGroupedStatistics(
            @Param("status") String status,
            @Param("itemType") String itemType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("dateFormatPattern") String dateFormatPattern
    );

    /**
     * Lấy thống kê tổng quan doanh thu từ mua EPISODE và COMBO
     */
    @Query(value = """
        SELECT
            'OVERVIEW' AS period,
            COALESCE(SUM(o.total_amount), 0) AS grossRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.coin_amount), 0) AS coinAmount,
            COALESCE(SUM(rt.amount), 0) AS creatorShareAmount,
            COALESCE(SUM(
                o.total_amount 
                - COALESCE(o.vat_amount, 0) 
                - COALESCE(o.coin_amount, 0) 
                - COALESCE(rt.amount, 0)
            ), 0) AS netRevenue
        FROM orders o
        LEFT JOIN revenue_transaction rt 
            ON o.order_id = rt.reference_id 
           AND rt.reference_type = 'ORDER' 
           AND rt.change_type = 'CONTENT_SHARE'
        WHERE o.status = :status
          AND o.item_type IN ('EPISODE', 'COMBO')
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        """, nativeQuery = true)
    ContentRevenueStatisticData getContentOverviewStatistic(
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy chi tiết doanh thu EPISODE và COMBO gom nhóm theo thời gian (Biểu đồ)
     */
    @Query(value = """
        SELECT
            TO_CHAR(o.created_at, :dateFormatPattern) AS period,
            COALESCE(SUM(o.total_amount), 0) AS grossRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.coin_amount), 0) AS coinAmount,
            COALESCE(SUM(rt.amount), 0) AS creatorShareAmount,
            COALESCE(SUM(
                o.total_amount 
                - COALESCE(o.vat_amount, 0) 
                - COALESCE(o.coin_amount, 0) 
                - COALESCE(rt.amount, 0)
            ), 0) AS netRevenue
        FROM orders o
        LEFT JOIN revenue_transaction rt 
            ON o.order_id = rt.reference_id 
           AND rt.reference_type = 'ORDER' 
           AND rt.change_type = 'CONTENT_SHARE'
        WHERE o.status = :status
          AND o.item_type IN ('EPISODE', 'COMBO')
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        GROUP BY 1
        ORDER BY period ASC
        """, nativeQuery = true)
    List<ContentRevenueStatisticData> getContentGroupedStatistics(
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("dateFormatPattern") String dateFormatPattern
    );

    /**
     * Lấy tổng quan doanh thu Premium / Subscription (COMPLETED)
     */
    @Query(value = """
        SELECT
            'OVERVIEW' AS period,
            COALESCE(SUM(o.total_amount), 0) AS grossRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.total_amount - COALESCE(o.vat_amount, 0)), 0) AS netRevenue
        FROM orders o
        WHERE o.status = :status
          AND o.item_type = :itemType
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        """, nativeQuery = true)
    SubscriptionStatisticData getSubscriptionOverviewStatistic(
            @Param("status") String status,
            @Param("itemType") String itemType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy chi tiết doanh thu Premium / Subscription gom nhóm động theo chuỗi thời gian (COMPLETED)
     */
    @Query(value = """
        SELECT
            TO_CHAR(o.created_at, :dateFormatPattern) AS period,
            COALESCE(SUM(o.total_amount), 0) AS grossRevenue,
            COALESCE(SUM(o.vat_amount), 0) AS vatAmount,
            COALESCE(SUM(o.total_amount - COALESCE(o.vat_amount, 0)), 0) AS netRevenue
        FROM orders o
        WHERE o.status = :status
          AND o.item_type = :itemType
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        GROUP BY 1
        ORDER BY period ASC
        """, nativeQuery = true)
    List<SubscriptionStatisticData> getSubscriptionGroupedStatistics(
            @Param("status") String status,
            @Param("itemType") String itemType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("dateFormatPattern") String dateFormatPattern
    );

    /**
     * Lấy danh sách chi tiết đơn hàng Campaign (ENGAGEMENT) phục vụ xuất Excel
     */
    @Query(value = """
        SELECT 
            o.order_id AS orderId,
            o.total_amount AS totalAmount,
            COALESCE(o.coin_amount, 0) AS coinAmount,
            COALESCE(o.vat_amount, 0) AS vatAmount,
            0 AS shareAmount,
            '' AS description,
            (o.total_amount - COALESCE(o.vat_amount, 0)) AS fiatAmount,
            o.status AS status,
            o.created_at AS createdAt,
            o.updated_at AS updatedAt,
            o.item_type AS itemType,
            o.item_id AS itemId,
            COALESCE(es.name, '') AS itemName,
            CAST(a.account_id AS VARCHAR) AS accountId,
            a.email AS email,
            COALESCE(a.full_name, '') AS fullName
        FROM orders o
        JOIN accounts a ON o.account_id = a.account_id
        LEFT JOIN engagement_service es ON o.item_id = es.engagement_service_id
        WHERE o.status = :status
          AND o.item_type = :itemType
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<OrderDetailStatisticProjection> getCampaignOrderDetails(
            @Param("status") String status,
            @Param("itemType") String itemType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy danh sách chi tiết đơn hàng Premium (SUBSCRIPTION) phục vụ xuất Excel
     */
    @Query(value = """
        SELECT 
            o.order_id AS orderId,
            o.total_amount AS totalAmount,
            COALESCE(o.coin_amount, 0) AS coinAmount,
            COALESCE(o.vat_amount, 0) AS vatAmount,
            0 AS shareAmount,
            '' AS description,
            (o.total_amount - COALESCE(o.vat_amount, 0)) AS fiatAmount,
            o.status AS status,
            o.created_at AS createdAt,
            o.updated_at AS updatedAt,
            o.item_type AS itemType,
            o.item_id AS itemId,
            COALESCE(sub.tier, '') AS itemName,
            CAST(a.account_id AS VARCHAR) AS accountId,
            a.email AS email,
            COALESCE(a.full_name, '') AS fullName
        FROM orders o
        JOIN accounts a ON o.account_id = a.account_id
        LEFT JOIN subscriptions sub ON o.item_id = sub.subscription_id
        WHERE o.status = :status
          AND o.item_type = :itemType
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<OrderDetailStatisticProjection> getSubscriptionOrderDetails(
            @Param("status") String status,
            @Param("itemType") String itemType,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy danh sách chi tiết đơn hàng Content (EPISODE & COMBO) phục vụ xuất Excel
     */
    @Query(value = """
        SELECT 
            o.order_id AS orderId,
            o.total_amount AS totalAmount,
            COALESCE(o.coin_amount, 0) AS coinAmount,
            COALESCE(o.vat_amount, 0) AS vatAmount,
            COALESCE(rt.amount, 0) AS shareAmount,
            COALESCE(rt.description, '') AS description,
            (o.total_amount - COALESCE(o.vat_amount, 0) - COALESCE(o.coin_amount, 0) - COALESCE(rt.amount, 0)) AS fiatAmount,
            o.status AS status,
            o.created_at AS createdAt,
            o.updated_at AS updatedAt,
            o.item_type AS itemType,
            o.item_id AS itemId,
            CASE 
                WHEN o.item_type = 'EPISODE' THEN CONCAT('Tập ', ep.episode_number, ': ', ep.title, ' (Series: ', ser.title, ')')
                WHEN o.item_type = 'COMBO' THEN cb.title
                ELSE ''
            END AS itemName,
            CAST(a.account_id AS VARCHAR) AS accountId,
            a.email AS email,
            COALESCE(a.full_name, '') AS fullName
        FROM orders o
        JOIN accounts a ON o.account_id = a.account_id
        LEFT JOIN revenue_transaction rt 
            ON o.order_id = rt.reference_id 
           AND rt.reference_type = 'ORDER' 
           AND rt.change_type = 'CONTENT_SHARE'
        LEFT JOIN episodes ep ON o.item_type = 'EPISODE' AND o.item_id = ep.episode_id
        LEFT JOIN seasons sea ON ep.season_id = sea.season_id
        LEFT JOIN series ser ON sea.series_id = ser.series_id
        LEFT JOIN combo_episodes cb ON o.item_type = 'COMBO' AND o.item_id = cb.combo_id
        WHERE o.status = :status
          AND o.item_type IN ('EPISODE', 'COMBO')
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<OrderDetailStatisticProjection> getContentOrderDetails(
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    /**
     * Lấy danh sách chi tiết đơn hàng Content (EPISODE & COMBO) lọc theo itemId cụ thể có status COMPLETED
     */
    @Query(value = """
        SELECT 
            o.order_id AS orderId,
            o.total_amount AS totalAmount,
            COALESCE(o.coin_amount, 0) AS coinAmount,
            COALESCE(o.vat_amount, 0) AS vatAmount,
            COALESCE(rt.amount, 0) AS shareAmount,
            COALESCE(rt.description, '') AS description,
            (o.total_amount - COALESCE(o.vat_amount, 0) - COALESCE(o.coin_amount, 0) - COALESCE(rt.amount, 0)) AS fiatAmount,
            o.status AS status,
            o.created_at AS createdAt,
            o.updated_at AS updatedAt,
            o.item_type AS itemType,
            o.item_id AS itemId,
            CASE 
                WHEN o.item_type = 'EPISODE' THEN CONCAT('Tập ', ep.episode_number, ': ', ep.title, ' (Series: ', ser.title, ')')
                WHEN o.item_type = 'COMBO' THEN cb.title
                ELSE ''
            END AS itemName,
            CAST(a.account_id AS VARCHAR) AS accountId,
            a.email AS email,
            COALESCE(a.full_name, '') AS fullName
        FROM orders o
        JOIN accounts a ON o.account_id = a.account_id
        LEFT JOIN revenue_transaction rt 
            ON o.order_id = rt.reference_id 
           AND rt.reference_type = 'ORDER' 
           AND rt.change_type = 'CONTENT_SHARE'
        LEFT JOIN episodes ep ON o.item_type = 'EPISODE' AND o.item_id = ep.episode_id
        LEFT JOIN seasons sea ON ep.season_id = sea.season_id
        LEFT JOIN series ser ON sea.series_id = ser.series_id
        LEFT JOIN combo_episodes cb ON o.item_type = 'COMBO' AND o.item_id = cb.combo_id
        WHERE o.status = :status
          AND o.item_type IN ('EPISODE', 'COMBO')
          AND o.item_id = :itemId
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<OrderDetailStatisticProjection> getContentOrderDetailsByItemId(
            @Param("itemId") String itemId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );

    @Query(value = """
        SELECT 
            o.order_id AS orderId,
            o.total_amount AS totalAmount,
            COALESCE(o.coin_amount, 0) AS coinAmount,
            COALESCE(o.vat_amount, 0) AS vatAmount,
            COALESCE(rt.amount, 0) AS shareAmount,
            COALESCE(rt.description, '') AS description,
            (o.total_amount - COALESCE(o.vat_amount, 0) - COALESCE(o.coin_amount, 0) - COALESCE(rt.amount, 0)) AS fiatAmount,
            o.status AS status,
            o.created_at AS createdAt,
            o.updated_at AS updatedAt,
            o.item_type AS itemType,
            o.item_id AS itemId,
            CONCAT('Tập ', ep.episode_number, ': ', ep.title, ' (Series: ', ser.title, ')') AS itemName,
            CAST(a.account_id AS VARCHAR) AS accountId,
            a.email AS email,
            COALESCE(a.full_name, '') AS fullName
        FROM orders o
        JOIN accounts a ON o.account_id = a.account_id
        LEFT JOIN revenue_transaction rt 
            ON o.order_id = rt.reference_id 
           AND rt.reference_type = 'ORDER' 
           AND rt.change_type = 'CONTENT_SHARE'
        JOIN episodes ep ON o.item_type = 'EPISODE' AND o.item_id = ep.episode_id
        JOIN seasons sea ON ep.season_id = sea.season_id
        JOIN series ser ON sea.series_id = ser.series_id
        WHERE o.status = :status
          AND ser.series_id = :seriesId
          AND o.created_at >= :startTime
          AND o.created_at <= :endTime
        ORDER BY o.created_at DESC
        """, nativeQuery = true)
    List<OrderDetailStatisticProjection> getContentOrderDetailsBySeriesId(
            @Param("seriesId") String seriesId,
            @Param("status") String status,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime
    );
}
