package com.talex.server.entities;

import com.talex.server.enums.CensorshipStatus;
import jakarta.persistence.CascadeType;
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
        name = "content_censorship",
        indexes = {
                @Index(name = "idx_censorship_media", columnList = "media_id"),
                @Index(name = "idx_censorship_status", columnList = "status")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ContentCensorship extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "censorship_id")
    private String censorshipId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "media_id", nullable = false)
    private Media media;

    // Raw JSON response from moderation provider (e.g. AWS Rekognition)
    @Column(name = "raw_response", columnDefinition = "TEXT")
    private String rawResponse;

    @Column(name = "primary_violation_label")
    private String primaryViolationLabel;

    @Column(name = "confidence_score")
    private Float confidenceScore;

    @Column(name = "checked_at")
    private LocalDateTime checkedAt;

    // "AWS_REKOGNITION" or "HUMAN" reviewer identifier
    @Column(name = "reviewed_by", length = 50)
    private String reviewedBy;

    @Column(name = "reviewer_notes", columnDefinition = "TEXT")
    private String reviewerNotes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private CensorshipStatus status = CensorshipStatus.PENDING;

    @OneToMany(mappedBy = "censorship", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ViolationDetail> violationDetails = new ArrayList<>();
}
