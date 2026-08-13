package com.talex.server.entities.ads;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.UUID;

@Entity
@Table(name = "ad_metrics", indexes = {
        @Index(name = "idx_ad_metric_report_date", columnList = "report_date")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdMetric {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "metric_id")
    private UUID metricId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private AdCampaign campaign;

    @Column(name = "report_date", nullable = false)
    private LocalDate reportDate;

    @Column(name = "impressions", nullable = false)
    @Builder.Default
    private Long impressions = 0L;

    @Column(name = "clicks", nullable = false)
    @Builder.Default
    private Long clicks = 0L;

    @Column(name = "focused_views_6s", nullable = false)
    @Builder.Default
    private Long focusedViews6s = 0L;


}
