package com.talex.server.entities;

import com.talex.server.enums.AccountSubscriptionStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDate;
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

    @Column(name = "is_ad_blocked", nullable = false)
    @Builder.Default
    private Boolean isAdBlocked = false;

    @Column(name = "is_movie_unlocked", nullable = false)
    @Builder.Default
    private Boolean isMovieUnlocked = false;

    @Column(name = "is_story_unlocked", nullable = false)
    @Builder.Default
    private Boolean isStoryUnlocked = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private AccountSubscriptionStatus status;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "subscription_id", nullable = false)
    private Subscription subscription;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    public boolean isExpired() {
        return endTime.isBefore(LocalDateTime.now());
    }
}
