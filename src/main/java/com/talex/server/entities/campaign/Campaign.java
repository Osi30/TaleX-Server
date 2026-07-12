package com.talex.server.entities.campaign;

import com.talex.server.entities.AnalyticData;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.engagement.CampaignStatus;
import com.talex.server.enums.engagement.EngagementTarget;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "campaign")
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class Campaign implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_id")
    private String campaignId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.RUNNING;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "target_value")
    private Long targetValue;

    @Column(name = "current_value")
    private Long currentValue = 0L;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "account_id")
    private UUID accountId;

    @Column(name = "order_id")
    private String orderId;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();

    @Enumerated(EnumType.STRING)
    @Column(name = "engagement_target", nullable = false)
    private EngagementTarget engagementTarget;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CampaignSeries> campaignSeries = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engagement_service_id", nullable = false)
    private EngagementService engagementService;

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CampaignLog> campaignLogs = new ArrayList<>();

    public void addSeries(Series series) {
        CampaignSeries campaignSeriesObj = CampaignSeries.builder()
                .campaign(this)
                .series(series)
                .build();
        this.campaignSeries.add(campaignSeriesObj);
    }
}
