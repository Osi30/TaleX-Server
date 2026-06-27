package com.talex.server.entities.creator;

import com.talex.server.entities.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "creator")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Creator {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_id")
    private String creatorId;

    @Column(name = "follower_count", nullable = false)
    @Builder.Default
    private Long followerCount = 0L;

    @Column(name = "total_views", nullable = false)
    @Builder.Default
    private Long totalViews = 0L;

    // Tính theo giờ
    @Column(name = "total_watch_time", nullable = false)
    @Builder.Default
    private Double totalWatchTime = 0D;

    @Column(name = "total_likes")
    @Builder.Default
    private Long likes = 0L;

    @Column(name = "total_bookmarks")
    @Builder.Default
    private Long bookmarks = 0L;

    @Column(name = "total_shares")
    @Builder.Default
    private Long shares = 0L;

    @Column(name = "total_comments")
    @Builder.Default
    private Long comments = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", nullable = false, unique = true)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_tier_id", nullable = false)
    private CreatorTier creatorTier;

    @OneToOne(mappedBy = "creator")
    private CreatorIdentity creatorIdentity;

    @OneToMany(mappedBy = "creator", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PaymentProfile> paymentProfiles = new ArrayList<>();
}
