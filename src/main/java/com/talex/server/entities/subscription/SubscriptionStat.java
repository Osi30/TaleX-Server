package com.talex.server.entities.subscription;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "subscription_stats")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionStat {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    @Column(name = "creator_id", nullable = false, length = 50)
    private String creatorId;

    @Column(name = "views")
    @Builder.Default
    private Long views = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_subscription_id", nullable = false)
    private AccountSubscription accountSubscription;
}
