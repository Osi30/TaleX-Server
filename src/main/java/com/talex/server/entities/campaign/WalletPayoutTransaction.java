package com.talex.server.entities.campaign;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.talex.server.enums.BankBin;
import com.talex.server.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "wallet_payout_transactions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WalletPayoutTransaction {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "wallet_payout_transaction_id")
    private String walletPayoutTransactionId;

    // 1. Mã do hệ thống tự sinh gửi sang PayOS
    @Column(name = "batch_reference_id")
    private String batchReferenceId; // Ref của Lô (data.referenceId)

    @Column(name = "transaction_reference_id")
    private String transactionReferenceId; // Ref của từng GD (transactions[i].referenceId)

    // 2. Mã do PayOS trả về
    @Column(name = "gateway_batch_id")
    private String gatewayBatchId; // Mã Lô từ PayOS

    @Column(name = "payout_reference")
    private String payoutReference; // Mã giao dịch đơn lẻ từ PayOS

    // 3. Thông tin số tiền & Trạng thái
    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
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
    @Enumerated(EnumType.STRING)
    private BankBin toBin;

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
    @JoinColumn(name = "payout_request_id", nullable = false)
    private PayoutRequest payoutRequest;
}