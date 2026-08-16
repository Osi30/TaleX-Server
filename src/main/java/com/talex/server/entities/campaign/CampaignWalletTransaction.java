package com.talex.server.entities.campaign;

import com.talex.server.enums.engagement.WalletReferenceType;
import com.talex.server.enums.engagement.WalletTransactionType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_wallet_transaction")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignWalletTransaction {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "transaction_id")
    private String transactionId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "wallet_id", nullable = false)
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    private CampaignWallet campaignWallet;

    // Số tiền biến động (+1000 khi refund, -1000 khi dùng thanh toán)
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    // Lưu vết số dư trước & sau giao dịch để dễ đối soát (Audit trail)
    @Column(name = "balance_before", nullable = false)
    private BigDecimal balanceBefore;

    @Column(name = "balance_after", nullable = false)
    private BigDecimal balanceAfter;

    @Enumerated(EnumType.STRING)
    @Column(name = "transaction_type", nullable = false)
    private WalletTransactionType transactionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "reference_type", nullable = false)
    private WalletReferenceType referenceType;

    // UUID dạng String của Campaign ID hoặc Order ID tương ứng
    @Column(name = "reference_id", nullable = false)
    private String referenceId;

    @Column(name = "description")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;
}