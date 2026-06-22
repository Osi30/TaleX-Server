package com.talex.server.entities.coin;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Đại diện cho Ví Coin của một tài khoản.
 * <p>
 * - Mỗi tài khoản chỉ có DUY NHẤT một ví (ràng buộc unique trên account_id).
 * - Ví chỉ được khởi tạo khi user phát sinh giao dịch nhận coin lần đầu tiên (Lazy Init).
 * - Kế thừa BaseAudit để ghi lại lịch sử tạo/sửa/xoá mềm.
 * </p>
 */
@Entity
@Table(name = "coin_wallets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinWallet extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_id", updatable = false, nullable = false)
    private UUID walletId;

    /**
     * Logical FK trỏ đến bảng accounts.
     * Không dùng @ManyToOne để giữ tính độc lập module.
     */
    @Column(name = "account_id", nullable = false, unique = true)
    private UUID accountId;

    /**
     * Số dư hiện tại của ví. DECIMAL(19,4) để đảm bảo độ chính xác tài chính.
     */
    @Column(name = "balance", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal balance = BigDecimal.ZERO;

    /**
     * Tổng số coin đã nhận vào từ trước đến nay.
     */
    @Column(name = "total_earned", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalEarned = BigDecimal.ZERO;

    /**
     * Tổng số coin đã chi ra từ trước đến nay.
     */
    @Column(name = "total_spent", nullable = false, precision = 19, scale = 4)
    @Builder.Default
    private BigDecimal totalSpent = BigDecimal.ZERO;
}
