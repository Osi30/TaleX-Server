package com.talex.server.entities.subscription;

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
@Table(name = "subscriptions")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Subscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "subscription_id")
    private String subscriptionId;

    @Column(name = "tier", nullable = false)
    private String tier;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "duration", nullable = false)
    private Integer duration;

    @Column(name = "duration_unit", nullable = false)
    private String durationUnit;

    @Column(name = "total_purchases", nullable = false)
    @Builder.Default
    private BigDecimal totalPurchases = BigDecimal.ZERO;

    @Column(name = "is_deleted", nullable = false)
    @Builder.Default
    private Boolean isDeleted = false;

    @Column(name = "is_ad_blocked", nullable = false)
    @Builder.Default
    private Boolean isAdBlocked = false;

    @Column(name = "is_movie_unlocked", nullable = false)
    @Builder.Default
    private Boolean isMovieUnlocked = false;

    @Column(name = "is_story_unlocked", nullable = false)
    @Builder.Default
    private Boolean isStoryUnlocked = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "subscription", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccountSubscription> accountSubscriptions = new ArrayList<>();
}
