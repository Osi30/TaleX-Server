package com.talex.server.entities.creator;

import com.talex.server.entities.Account;
import com.talex.server.entities.AnalyticData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "creator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "is_verified")
    @Builder.Default
    private Boolean isVerified = false;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();

    // Tổng quan doanh thu
    @Column(name = "current_balance", nullable = false)
    @Builder.Default
    private BigDecimal currentBalance = BigDecimal.ZERO;

    @Column(name = "total_balance", nullable = false)
    @Builder.Default
    private BigDecimal totalBalance = BigDecimal.ZERO;

    @Column(name = "estimated_net_balance", nullable = false)
    @Builder.Default
    private BigDecimal estimatedNetBalance = BigDecimal.ZERO;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_tier_id", nullable = false)
    private CreatorTier creatorTier;

    @OneToOne(mappedBy = "creator")
    private CreatorIdentity creatorIdentity;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentProfile> paymentProfiles = new ArrayList<>();

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<RevenueTransaction> revenueTransactions = new ArrayList<>();
}
