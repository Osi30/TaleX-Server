package com.talex.server.entities.campaign;

import com.talex.server.entities.AnalyticData;
import com.talex.server.entities.series.Episode;
import com.talex.server.entities.series.Series;
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
    private String campaignEpisodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();
}