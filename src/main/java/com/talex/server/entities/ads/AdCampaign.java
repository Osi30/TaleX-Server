package com.talex.server.entities.ads;

import com.talex.server.enums.ads.AdCampaignStatus;
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
import java.util.UUID;

@Entity
@Table(name = "ad_campaigns", indexes = {
        @Index(name = "idx_ad_campaign_status", columnList = "status"),
        @Index(name = "idx_ad_campaign_slot_id", columnList = "slot_id")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdCampaign {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_id")
    private UUID campaignId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "profile_id", nullable = false)
    private AdvertiseProfile profile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "slot_id", nullable = false)
    private AdSlot slot;

    @Column(name = "name", nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private AdCampaignStatus status = AdCampaignStatus.PENDING_REVIEW;

    @Column(name = "target_impressions", nullable = false)
    @Builder.Default
    private Long targetImpressions = 0L;

    @Column(name = "target_clicks", nullable = false)
    @Builder.Default
    private Long targetClicks = 0L;

    @Column(name = "current_impressions", nullable = false)
    @Builder.Default
    private Long currentImpressions = 0L;

    @Column(name = "current_clicks", nullable = false)
    @Builder.Default
    private Long currentClicks = 0L;

    @Column(name = "focused_views_6s", nullable = false)
    @Builder.Default
    private Long focusedViews6s = 0L;

    @Column(name = "paid_focused_views_6s", nullable = false)
    @Builder.Default
    private Long paidFocusedViews6s = 0L;

    @Column(name = "total_budget", nullable = false)
    private Long totalBudget;

    @Column(name = "campaign_balance", nullable = false)
    @Builder.Default
    private Long campaignBalance = 0L;

    @Column(name = "locked_cpm")
    private Long lockedCpm;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "start_date")
    private LocalDateTime startDate;

    @Column(name = "end_date")
    private LocalDateTime endDate;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdCreative> creatives = new ArrayList<>();

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdMetric> metrics = new ArrayList<>();
    
    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AdTransaction> transactions = new ArrayList<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "ad_campaign_labels", joinColumns = @JoinColumn(name = "campaign_id"))
    @Column(name = "label_name")
    @Builder.Default
    private List<String> labels = new ArrayList<>();

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}
