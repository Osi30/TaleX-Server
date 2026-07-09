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
@Table(name = "campaign_episode_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"campaign_episode_id", "hour_bucket"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignEpisodeLog implements Serializable {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_episode_log_id")
    private String campaignEpisodeLogId;

    @Column(name = "hour_bucket", nullable = false)
    private LocalDateTime hourBucket;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_episode_id", nullable = false)
    private CampaignEpisode campaignEpisode;

    @Embedded
    @Builder.Default
    private AnalyticData analyticData = new AnalyticData();
}