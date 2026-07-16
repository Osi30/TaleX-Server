package com.talex.server.entities.series;

import com.talex.server.entities.AnalyticData;
import com.talex.server.entities.BaseTimeEntity;
import com.talex.server.entities.creator.Creator;
import com.talex.server.enums.series.ContentType;
import com.talex.server.enums.series.SeriesStatus;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(
        name = "series",
        indexes = {
                @Index(name = "idx_series_creator_deleted", columnList = "creator_id,is_deleted"),
                @Index(name = "idx_series_public_listing", columnList = "status,is_deleted")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class Series extends BaseTimeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "series_id")
    private String seriesId;

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

    @Column(name = "age_rating", length = 50)
    private String ageRating;

    @Column(length = 20)
    private String language;

    @Column(name = "total_rating", nullable = false, columnDefinition = "float8 default 0.0")
    private Double totalRating = 0D;

    @Column(name = "rating_count", nullable = false, columnDefinition = "bigint default 0")
    private Long ratingCount = 0L;

    @Column(name = "average_rating", nullable = false, columnDefinition = "float8 default 0.0")
    private Double averageRating = 0.0;

    @Embedded
    private AnalyticData analyticData = new AnalyticData();

    // Dành cho tính năng lưu trữ động
    @Column(name = "last_interaction_time")
    private LocalDateTime lastInteractionTime;

    @Column(name = "is_24h_sync", nullable = false)
    private boolean is24hSync = true;

    @Column(name = "is_7d_sync", nullable = false)
    private boolean is7dSync = true;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private Creator creator;

    @OneToMany(mappedBy = "series")
    private List<SeriesCategory> seriesCategories = new ArrayList<>();

    @OneToMany(mappedBy = "series")
    private List<SeriesTag> seriesTags = new ArrayList<>();

    @OneToMany(mappedBy = "series")
    private List<Season> seasons = new ArrayList<>();
}
