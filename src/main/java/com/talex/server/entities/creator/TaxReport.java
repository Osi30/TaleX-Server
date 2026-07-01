package com.talex.server.entities.creator;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "tax_report")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaxReport {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "tax_report_id")
    private UUID taxReportId;

    @Column(name = "tax_amount", nullable = false)
    private BigDecimal taxAmount;

    @Column(name = "quarter")
    private Integer quarter;

    @Column(name = "year", nullable = false)
    private Integer year;

    @Column(name = "month")
    private Integer month;

    @Column(name = "state_payment_receipt_id")
    private String statePaymentReceiptId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_monthly_settlement_id", nullable = false)
    private CreatorMonthlySettlement creatorMonthlySettlement;
}