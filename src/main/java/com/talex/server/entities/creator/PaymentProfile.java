package com.talex.server.entities.creator;

import com.talex.server.enums.creator.PaymentProfileStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "payment_profile")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PaymentProfile {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "payment_profile_id")
    private String paymentProfileId;

    @Column(name = "bank_code", length = 50)
    private String bankCode;

    @Column(name = "account_number", length = 50)
    private String accountNumber;

    @Column(name = "account_name", length = 200)
    private String accountName;

    @Column(name = "is_primary")
    private Boolean isPrimary = false;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "verified_note")
    private String verifiedNote;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", length = 20)
    private PaymentProfileStatus status;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
}
