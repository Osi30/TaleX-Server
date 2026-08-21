package com.talex.server.entities.config;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "settlement_config")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SettlementConfig {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "min_balance_threshold")
    private BigDecimal minBalanceThreshold;

    @Column(name = "min_payout_threshold")
    private BigDecimal minPayoutThreshold;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}