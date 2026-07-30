package com.talex.server.entities.campaign;

import com.talex.server.entities.analytic.AnalyticData;
import com.talex.server.entities.series.Series;
import com.talex.server.enums.engagement.CampaignStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Entity
@Table(name = "campaign_series")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSeries implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_series_id")
    private String campaignSeriesId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private CampaignStatus status = CampaignStatus.RUNNING;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();

    @Column(name = "total_impression")
    @Builder.Default
    private Long totalImpression = 0L;
}