package com.talex.server.entities.report;

import com.talex.server.enums.report.AppealStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "appeals")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Appeal {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "appeal_id")
    private String appealId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "penalty_id", nullable = false, unique = true)
    private Penalty penalty;

    @Column(name = "appellant_id", nullable = false)
    private String appellantId;

    @Column(name = "reviewer_id")
    private String reviewerId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Column(name = "proof_documents", columnDefinition = "TEXT")
    private String proofDocuments;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private AppealStatus status = AppealStatus.PENDING;

    @Column(name = "admin_note", columnDefinition = "TEXT")
    private String adminNote;

    @Column(name = "resolved_at")
    private LocalDateTime resolvedAt;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
}