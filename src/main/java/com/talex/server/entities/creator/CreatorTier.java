package com.talex.server.entities.creator;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "creator_tier")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreatorTier {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "creator_tier_id")
    private String creatorTierId;

    @Column(name = "tier_name", length = 100, nullable = false)
    private String tierName;

    @Column(name = "tier_level", nullable = false)
    private Integer tierLevel = -1;

    @Column(name = "min_follower_required", nullable = false)
    private Long minFollowerRequired = 0L;

    @Column(name = "min_views_required", nullable = false)
    private Long minViewsRequired = 0L;

    // Tính theo giờ
    @Column(name = "min_watch_time_required", nullable = false)
    private Double minWatchTimeRequired = 0D;

    @Column(name = "premium_fund_share_ratio", nullable = false)
    private Double premiumFundShareRatio;

    @Column(name = "direct_purchase_share_ratio", nullable = false)
    private Double directPurchaseShareRatio;

    // Tier mặc định khi tạo mới tài khoản
    @Column(name = "is_default", nullable = false)
    private Boolean isDefault = false;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}