package com.talex.server.entities.campaign;

import com.talex.server.entities.creator.PaymentProfile;
import com.talex.server.enums.BankBin;
import com.talex.server.enums.engagement.PayoutRequestStatus;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "payout_requests")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PayoutRequest {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payout_request_id")
    private String payoutRequestId;

    @Column(name = "account_id", nullable = false)
    private UUID accountId;

    @Column(name = "amount", nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private PayoutRequestStatus status = PayoutRequestStatus.PENDING;

    // Snapshot thông tin ngân hàng tại thời điểm tạo yêu cầu
    @Column(name = "payment_profile_id", nullable = false)
    private String paymentProfileId;

    @Column(name = "bank_name")
    @Enumerated(EnumType.STRING)
    private BankBin bankName;

    @Column(name = "bank_account_number")
    private String bankAccountNumber;

    @Column(name = "bank_account_name")
    private String bankAccountName;

    @Column(name = "admin_note")
    private String adminNote;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}