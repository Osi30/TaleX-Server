package com.talex.server.entities.subscription;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "subscription_results")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubscriptionResult {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private String id;

    @Column(name = "alpha")
    private Double alpha;

    @Column(name = "gamma", nullable = false)
    private Double gamma;

    @Column(name = "total_budget")
    private Double totalBudget;

    // Đã trừ VAT
    @Column(name = "target_budget", nullable = false)
    private Double targetBudget;

    // Đã trừ VAT và user không xem
    @Column(name = "calculated_budget", nullable = false)
    private Double calculatedBudget;

    @Column(name = "month_year", nullable = false, length = 7)
    private String monthYear;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @OneToMany(mappedBy = "subscriptionResult", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<SubscriptionRevenueLog> revenueLogs = new ArrayList<>();
}