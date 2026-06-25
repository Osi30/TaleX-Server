package com.talex.server.entities;

import com.talex.server.enums.ContentApprovalStatus;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.SeriesStatus;
import com.talex.server.enums.Visibility;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "series",
        indexes = {
                @Index(name = "idx_series_creator_deleted", columnList = "creator_id,is_deleted"),
                @Index(name = "idx_series_public_listing", columnList = "visibility,status,approval_status,is_deleted"),
                @Index(name = "idx_series_schedule_publish", columnList = "approval_status,scheduled_publish_at,status,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Series extends BaseAudit {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "series_id")
    private String seriesId;

    @Column(name = "creator_id")
    private String creatorId;

    @Column(nullable = false, length = 250)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(name = "cover_url", columnDefinition = "TEXT")
    private String coverUrl;

    @Column(name = "banner_url", columnDefinition = "TEXT")
    private String bannerUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "content_type", nullable = false, length = 30)
    private ContentType contentType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeriesStatus status = SeriesStatus.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(name = "approval_status", nullable = false, length = 30)
    private ContentApprovalStatus approvalStatus = ContentApprovalStatus.PENDING_REVIEW;

    @Column(name = "approval_reviewed_at")
    private LocalDateTime approvalReviewedAt;

    @Column(name = "approval_reviewed_by")
    private String approvalReviewedBy;

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Visibility visibility = Visibility.PRIVATE;

    @Column(name = "age_rating", length = 50)
    private String ageRating;

    @Column(length = 20)
    private String language;

    @Column(name = "total_views", nullable = false)
    private Long totalViews = 0L;

    @Column(name = "total_subscriptions", nullable = false)
    private Long totalSubscriptions = 0L;

    @OneToMany(mappedBy = "series")
    private List<SeriesCategory> seriesCategories = new ArrayList<>();

    @OneToMany(mappedBy = "series")
    private List<SeriesTag> seriesTags = new ArrayList<>();

    @OneToMany(mappedBy = "series")
    private List<Season> seasons = new ArrayList<>();
}
