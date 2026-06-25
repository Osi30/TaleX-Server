package com.talex.server.entities;

import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.SeasonStatus;
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
        name = "seasons",
        indexes = {
                @Index(name = "idx_seasons_series_status_approval_deleted", columnList = "series_id,status,approval_status,is_deleted"),
                @Index(name = "idx_seasons_schedule_publish", columnList = "approval_status,scheduled_publish_at,status,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Season extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "season_id")
    private String seasonId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private Series series;

    @Column(name = "season_number", nullable = false)
    private Integer seasonNumber;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeasonStatus status = SeasonStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ContentApprovalStatus approvalStatus = ContentApprovalStatus.PENDING_REVIEW;

    @Column(name = "approval_reviewed_at")
    private LocalDateTime approvalReviewedAt;

    @Column(name = "approval_reviewed_by")
    private String approvalReviewedBy;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @OneToMany(mappedBy = "season")
    private List<Episode> episodes = new ArrayList<>();
}
