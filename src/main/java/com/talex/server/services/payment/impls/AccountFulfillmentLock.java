package com.talex.server.services.payment.impls;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Component;

import java.util.UUID;

/**
 * Postgres transaction-scoped advisory lock ({@code pg_advisory_xact_lock}) theo
 * accountId — tự động release khi transaction hiện tại commit/rollback, không cần
 * quản lý release thủ công. Dùng để serialize các thao tác fulfill Order (unlock
 * nội dung episode/combo, tạo subscription) của CÙNG 1 account chạy trên các Order
 * KHÁC NHAU đồng thời.
 * <p>
 * Order-row lock ({@code findWithLockByOrderId}/{@code findWithLockByPaymentCode})
 * chỉ chặn xử lý trùng lặp trên CÙNG 1 order — không chặn 2 order khác nhau của
 * cùng account chạy song song (VD mua lẻ tập + mua combo chứa tập đó, hoặc 2 order
 * subscription hoàn tất cùng lúc). Method này PHẢI được gọi từ bên trong một
 * transaction đang mở (join transaction hiện tại của caller), nếu không advisory
 * lock sẽ tự release ngay lập tức (do auto-commit) và không có tác dụng gì.
 */
@Component
public class AccountFulfillmentLock {

    @PersistenceContext
    private EntityManager entityManager;

    public void acquire(UUID accountId) {
        entityManager.createNativeQuery("SELECT pg_advisory_xact_lock(:key)")
                .setParameter("key", accountId.getMostSignificantBits())
                .getSingleResult();
    }
}
