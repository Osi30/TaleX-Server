package com.talex.server.entities.campaign;

import com.talex.server.entities.Account;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "campaign_log")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CampaignLog {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "campaign_log_id")
    private String campaignLogId;

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
    @JoinColumn(name = "campaign_id", nullable = false)
    private Campaign campaign;
}
