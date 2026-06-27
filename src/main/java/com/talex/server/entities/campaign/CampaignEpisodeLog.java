package com.talex.server.entities.campaign;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_episode_log", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"campaign_episode_id", "hour_bucket"})
})
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignEpisodeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_episode_log_id")
    private String campaignEpisodeLogId;

    @Column(name = "hour_bucket", nullable = false)
    private LocalDateTime hourBucket;

    @Column(name = "views")
    private Long views = 0L;

    @Column(name = "likes")
    private Long likes = 0L;

    @Column(name = "bookmarks")
    private Long bookmarks = 0L;

    @Column(name = "shares")
    private Long shares = 0L;

    @Column(name = "comments")
    private Long comments = 0L;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "campaign_episode_id", nullable = false)
    private CampaignEpisode campaignEpisode;
}