package com.talex.server.entities.campaign;

import com.talex.server.entities.AnalyticData;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.engagement.CampaignStatus;
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

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Column(name = "target_impression")
    private Long targetImpression;

    @Column(name = "current_impression")
    private Long currentImpression = 0L;

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

    @OneToMany(mappedBy = "campaign", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<CampaignSeries> campaignSeries = new ArrayList<>();

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "engagement_service_id", nullable = false)
    private EngagementService engagementService;

    public void addSeries(Series series) {
        CampaignSeries campaignSeriesObj = CampaignSeries.builder()
                .campaign(this)
                .series(series)
                .status(CampaignStatus.RUNNING)
                .build();
        this.campaignSeries.add(campaignSeriesObj);
    }

    public CampaignStatus getCampaignStatus() {
        // Default
        if (campaignSeries == null || campaignSeries.isEmpty()) {
            return CampaignStatus.UNAVAILABLE;
        }

        // Có ít nhất 1 Series đang RUNNING -> Campaign là RUNNING
        boolean hasRunning = campaignSeries.stream()
                .anyMatch(cs -> cs.getStatus() == CampaignStatus.RUNNING);
        if (hasRunning) {
            return CampaignStatus.RUNNING;
        }

        // Tất cả Series đều là COMPLETED -> Campaign là COMPLETED
        boolean allCompleted = campaignSeries.stream()
                .allMatch(cs -> cs.getStatus() == CampaignStatus.COMPLETED);
        if (allCompleted) {
            return CampaignStatus.COMPLETED;
        }

        // Tất cả Series đều là CANCELLED -> Campaign là CANCELLED
        boolean allCancelled = campaignSeries.stream()
                .allMatch(cs -> cs.getStatus() == CampaignStatus.CANCELLED);
        if (allCancelled) {
            return CampaignStatus.CANCELLED;
        }

        // Tất cả Series đều là PAUSED -> Campaign là PAUSED
        boolean allPaused = campaignSeries.stream()
                .allMatch(cs -> cs.getStatus() == CampaignStatus.PAUSED);
        if (allPaused) {
            return CampaignStatus.PAUSED;
        }

        return CampaignStatus.UNAVAILABLE;
    }

    public void updateStatus(CampaignStatus targetStatus) {
        if (this.campaignSeries == null || targetStatus == null) {
            return;
        }

        for (CampaignSeries series : this.campaignSeries) {
            CampaignStatus currentStatus = series.getStatus();

            // 1. Nếu đang là RUNNING -> Cho phép sang PAUSED, CANCELLED, COMPLETED
            if (currentStatus == CampaignStatus.RUNNING) {
                if (targetStatus == CampaignStatus.PAUSED ||
                        targetStatus == CampaignStatus.CANCELLED ||
                        targetStatus == CampaignStatus.COMPLETED) {
                    series.setStatus(targetStatus);
                }
            }
            // 2. Nếu đang là PAUSED -> Cho phép sang RUNNING, COMPLETED
            else if (currentStatus == CampaignStatus.PAUSED) {
                if (targetStatus == CampaignStatus.RUNNING ||
                        targetStatus == CampaignStatus.COMPLETED) {
                    series.setStatus(targetStatus);
                }
            }
        }
    }
}
