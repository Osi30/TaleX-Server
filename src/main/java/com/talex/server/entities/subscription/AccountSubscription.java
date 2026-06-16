package com.talex.server.entities.subscription;

import com.talex.server.entities.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "account_subscription")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AccountSubscription {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "account_subscription_id")
    private String accountSubscriptionId;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Column(name = "cancelled_at")
    private LocalDateTime cancelledAt;

    @Column(name = "is_ad_blocked", nullable = false)
    @Builder.Default
    private Boolean isAdBlocked = false;

    @Column(name = "is_movie_unlocked", nullable = false)
    @Builder.Default
    private Boolean isMovieUnlocked = false;

    @Column(name = "is_story_unlocked", nullable = false)
    @Builder.Default
    private Boolean isStoryUnlocked = false;

    @Column(name = "is_cancelled", nullable = false)
    @Builder.Default
    private Boolean isCancelled = false;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
