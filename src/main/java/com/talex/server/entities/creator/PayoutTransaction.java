package com.talex.server.entities.creator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.talex.server.enums.BankBin;
import com.talex.server.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "payout_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payout_transaction_id")
    private String payoutTransactionId;

    // 1. Mã do hệ thống mình tự sinh ra gửi sang PayOS
    @Column(name = "batch_reference_id")
    private String batchReferenceId; // ref của Lô (data.referenceId)

    @Column(name = "transaction_reference_id")
    private String transactionReferenceId; // ref của từng GD (transactions[i].referenceId)

    // 2. Mã do PayOS trả về
    @Column(name = "gateway_batch_id")
    private String gatewayBatchId; // Mã Lô của PayOS (data.id = payout_123456789)

    @Column(name = "payout_reference")
    private String payoutReference; // Mã GD đơn lẻ của PayOS (transactions[i].id = txn_123456789)

    // 3. Thông tin số tiền & Trạng thái
    @Column(name = "amount", nullable = false)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    private PayoutStatus status;

    @Column(name = "failure_reason", columnDefinition = "TEXT")
    private String failureReason;

    @Column(name = "paid_at")
    private LocalDateTime paidAt;

    // 4. Snapshot thông tin tài khoản nhận tiền
    @Column(name = "to_bin", length = 20)
    private BankBin toBin; // Mã BIN ngân hàng (VD: 970422 - MBBank)

    @Column(name = "to_account_number", length = 50)
    private String toAccountNumber;

    @Column(name = "to_account_name", length = 100)
    private String toAccountName;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_monthly_settlement_id", nullable = false)
    private CreatorMonthlySettlement creatorMonthlySettlement;
}