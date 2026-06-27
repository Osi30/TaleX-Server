package com.talex.server.entities.campaign;

import com.talex.server.entities.Episode;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_episode")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignEpisode {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_episode_id")
    private String campaignEpisodeId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "episode_id", nullable = false)
    private Episode episode;
}