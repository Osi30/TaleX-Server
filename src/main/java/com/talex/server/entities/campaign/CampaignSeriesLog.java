package com.talex.server.entities.campaign;

import com.talex.server.entities.AnalyticData;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_series_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"campaign_series_id", "hour_bucket"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignSeriesLog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_series_log_id")
    private String campaignEpisodeLogId;

    @Column(name = "hour_bucket", nullable = false)
    private LocalDateTime hourBucket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_series_id", nullable = false)
    private CampaignSeries campaignSeries;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();
}