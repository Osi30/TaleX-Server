package com.talex.server.entities.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "subscription_revenue_logs")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionRevenueLog {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "episode_id", nullable = false, length = 50)
    private String episodeId;

    @Column(name = "creator_id", nullable = false, length = 50)
    private String creatorId;

    @Column(name = "revenue", nullable = false)
    private Double revenue;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_result_id", nullable = false)
    private SubscriptionResult subscriptionResult;
}