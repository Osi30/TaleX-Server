package com.talex.server.entities.series;

import com.talex.server.entities.BaseAudit;
import com.talex.server.entities.Media;
import com.talex.server.enums.ContentType;
import com.talex.server.enums.EpisodeStatus;
import com.talex.server.enums.EpisodeUnlockType;
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
                @Index(name = "idx_episodes_season_status_deleted", columnList = "season_id,status,is_deleted"),
                @Index(name = "idx_episodes_schedule_publish_due", columnList = "scheduled_publish_at,status,is_deleted"),
                @Index(name = "idx_episodes_unlock_type", columnList = "unlock_type,is_deleted"),
                @Index(name = "idx_episodes_creator_deleted", columnList = "creator_id,is_deleted")
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

    @Column(name = "creator_id")
    private String creatorId;

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

    @Column(name = "scheduled_publish_at")
    private LocalDateTime scheduledPublishAt;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "unlock_type", nullable = false, length = 30, columnDefinition = "varchar(30) default 'FREE'")
    private EpisodeUnlockType unlockType = EpisodeUnlockType.FREE;

    @Column(name = "price_vnd", nullable = false, columnDefinition = "bigint default 0")
    private Long priceVnd = 0L;

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
