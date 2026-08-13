package com.talex.server.entities.creator;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.talex.server.enums.transaction.SettlementStatus;
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
@Table(name = "creator_monthly_settlement")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorMonthlySettlement {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_monthly_settlement_id")
    private String creatorMonthlySettlementId;

    @Column(name = "settlement_month", nullable = false)
    private String settlementMonth;

    @Column(name = "gross_amount", nullable = false)
    @Builder.Default
    private BigDecimal grossAmount = BigDecimal.ZERO;

    @Column(name = "tax_rate")
    private Double taxRate;

    @Column(name = "tax_withheld_amount")
    private BigDecimal taxWithheldAmount;

    @Column(name = "net_payout_amount", nullable = false)
    @Builder.Default
    private BigDecimal netPayoutAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private SettlementStatus status;

    @Column(name = "cutoff_date")
    private LocalDateTime cutoffDate;

    @Column(name = "total_penalty_amount")
    @Builder.Default
    private BigDecimal totalPenaltyAmount = BigDecimal.ZERO;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "note", columnDefinition = "TEXT")
    private String note;

    @JsonIgnore
    @OneToMany(mappedBy = "creatorMonthlySettlement")
    @Builder.Default
    private List<RevenueTransaction> revenueTransactions = new ArrayList<>();

    @JsonIgnore
    @OneToMany(mappedBy = "creatorMonthlySettlement", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<PayoutTransaction> payoutTransactions = new ArrayList<>();
}