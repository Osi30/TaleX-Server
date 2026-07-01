package com.talex.server.entities.media;

import com.talex.server.entities.BaseAudit;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

@Entity
@Table(
        name = "violation_detail",
        indexes = {
                @Index(name = "idx_violation_censorship", columnList = "censorship_id")
        })
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class ViolationDetail extends BaseAudit {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "violation_detail_id")
    private String violationDetailId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "censorship_id", nullable = false)
    private ContentCensorship censorship;

    // Timestamp in video (milliseconds); 0 for image violations
    @Column(name = "violation_at")
    private Float violationAt;

    @Column(name = "end_violation_at")
    private Float endViolationAt;

    // Moderation label (e.g. "Explicit Nudity", "Violence")
    @Column(length = 100)
    private String label;

    private Float confidence;

    @Column(columnDefinition = "TEXT")
    private String suggestion;
}
