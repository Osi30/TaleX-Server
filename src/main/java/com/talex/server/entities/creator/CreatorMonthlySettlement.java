package com.talex.server.entities.creator;

import com.talex.server.enums.transaction.SettlementStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

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

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;
}