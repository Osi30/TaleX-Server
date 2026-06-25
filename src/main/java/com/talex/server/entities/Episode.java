package com.talex.server.entities;

import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "episodes",
        indexes = {
                @Index(name = "idx_episodes_season_status_approval_deleted", columnList = "season_id,status,approval_status,is_deleted"),
                @Index(name = "idx_episodes_schedule_publish", columnList = "approval_status,scheduled_publish_at,status,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Episode extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "episode_id")
    private String episodeId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "season_id", nullable = false)
    private Season season;

    @Column(name = "episode_number", nullable = false)
    private Integer episodeNumber;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EpisodeStatus status = EpisodeStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ContentApprovalStatus approvalStatus = ContentApprovalStatus.PENDING_REVIEW;

    @Column(name = "approval_reviewed_at")
    private LocalDateTime approvalReviewedAt;

    @Column(name = "approval_reviewed_by")
    private String approvalReviewedBy;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(nullable = false)
    private Long likes = 0L;

    @Column(nullable = false)
    private Long views = 0L;

    @Column(name = "total_page")
    private Integer totalPage;

    @Column(name = "total_duration")
    private Double totalDuration = 0D;

    @OneToMany(mappedBy = "episode")
    private List<Media> media = new ArrayList<>();
}
