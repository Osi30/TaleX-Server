package com.talex.server.entities;

import com.talex.server.enums.ViolationType;
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
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "media_copyright",
        indexes = {
                @Index(name = "idx_media_copyright_media", columnList = "media_id"),
                @Index(name = "idx_media_copyright_source", columnList = "source_media_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class MediaCopyright extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "media_copyright_id")
    private String mediaCopyrightId;

    // Target video being checked for copyright violation
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    // Original video being copied from (nullable when source is unknown)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_media_id")
    private Media sourceMedia;

    @Column(name = "start_time_target")
    private Float startTimeTarget;

    @Column(name = "end_time_target")
    private Float endTimeTarget;

    @Column(name = "start_time_source")
    private Float startTimeSource;

    @Column(name = "end_time_source")
    private Float endTimeSource;

    @Column(name = "similarity_score")
    private Float similarityScore;

    @Enumerated(EnumType.STRING)
    @Column(name = "violation_type", length = 20)
    private ViolationType violationType;

    // true = violation is permissible (e.g. CC0 source license)
    @Column(name = "is_valid")
    private Boolean isValid = false;

    @Column(columnDefinition = "TEXT")
    private String note;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;
}
