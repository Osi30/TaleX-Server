package com.talex.server.entities.coin;

import com.talex.server.enums.CoinTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Bản ghi bất biến (Immutable) của một giao dịch coin.
 * <p>
 * - KHÔNG kế thừa BaseAudit vì transaction log không cần updatedAt, deletedAt, softDelete...
 * - Trường referenceId là Logical FK đa hình (polymorphic), KHÔNG dùng @ManyToOne/@JoinColumn.
 * - changedAt được Hibernate tự động điền khi INSERT, không cho phép UPDATE.
 * </p>
 */
@Entity
@Table(name = "coin_transactions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id", updatable = false, nullable = false)
    private UUID transactionId;

    /**
     * Logical FK trỏ đến bảng coin_wallets.
     * Không dùng @ManyToOne để tránh N+1 không kiểm soát và giữ tính độc lập.
     */
    @Column(name = "wallet_id", nullable = false, updatable = false)
    private UUID walletId;

    /**
     * Số coin thay đổi trong giao dịch này. Luôn là giá trị dương.
     * Chiều thay đổi được xác định bởi transactionType.
     */
    @Column(name = "amount", nullable = false, precision = 19, scale = 4)
    private BigDecimal amount;

    /**
     * Loại giao dịch: CREDIT (nhận) hoặc DEBIT (chi).
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false, length = 50)
    private CoinTransactionType transactionType;

    /**
     * Số dư ví TRƯỚC khi giao dịch này xảy ra. Dùng để audit và debug.
     */
    @Column(name = "balance_before", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceBefore;

    /**
     * Số dư ví SAU khi giao dịch này xảy ra.
     */
    @Column(name = "balance_after", nullable = false, precision = 19, scale = 4)
    private BigDecimal balanceAfter;

    /**
     * Phân loại nguồn gốc giao dịch (ví dụ: "DAILY_CHECK_IN", "PURCHASE", "MISSION_REWARD").
     * Dùng để filter trong lịch sử giao dịch.
     */
    @Column(name = "reference_type", length = 100)
    private String referenceType;

    /**
     * ID của bản ghi nguồn gốc (ví dụ: checkinId từ bảng daily_check_ins).
     * Là Logical FK đa hình — TUYỆT ĐỐI KHÔNG dùng @JoinColumn hay @ManyToOne.
     */
    @Column(name = "reference_id", length = 255)
    private String referenceId;

    /**
     * Mô tả ngắn gọn, thân thiện với người dùng về giao dịch này.
     */
    @Column(name = "description", length = 500)
    private String description;

    /**
     * Thời điểm giao dịch xảy ra. Tự động điền bởi Hibernate, không cho phép sửa.
     */
    @CreationTimestamp
    @Column(name = "changed_at", nullable = false, updatable = false)
    private LocalDateTime changedAt;
}
