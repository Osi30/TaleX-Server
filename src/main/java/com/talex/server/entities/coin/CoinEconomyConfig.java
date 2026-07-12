package com.talex.server.entities.coin;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Entity
@Table(name = "coin_economy_configs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CoinEconomyConfig extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "config_id", updatable = false, nullable = false)
    private UUID configId;

    @Column(name = "daily_check_in_base", nullable = false, precision = 19, scale = 4)
    private BigDecimal dailyCheckInBase;

    @Column(name = "milestone_7_reward", nullable = false, precision = 19, scale = 4)
    private BigDecimal milestone7Reward;

    @Column(name = "milestone_14_reward", nullable = false, precision = 19, scale = 4)
    private BigDecimal milestone14Reward;

    @Column(name = "milestone_30_reward", nullable = false, precision = 19, scale = 4)
    private BigDecimal milestone30Reward;

    @Column(name = "vnd_per_coin", nullable = false, precision = 19, scale = 4,
            columnDefinition = "numeric(19,4) default 100.0000")
    private BigDecimal vndPerCoin;
}
