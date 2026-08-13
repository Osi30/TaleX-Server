package com.talex.server.entities.report;

import com.talex.server.enums.report.PenaltyLevel;
import com.talex.server.enums.report.PenaltyStatus;
import com.talex.server.enums.report.TargetType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "penalties")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Penalty {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "penalty_id")
    private String penaltyId;

    @Column(name = "ticket_id")
    private String ticketId;

    @Column(name = "target_user_id", nullable = false)
    private String targetUserId;

    @Column(name = "issuer_id", nullable = false)
    private String issuerId;

    @Enumerated(EnumType.STRING)
    @Column(name = "level", nullable = false, length = 30)
    private PenaltyLevel level;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", nullable = false, length = 30)
    private TargetType targetType;

    @Column(name = "target_id", nullable = false)
    private String targetId;

    @Column(name = "reason", columnDefinition = "TEXT", nullable = false)
    private String reason;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 30)
    @Builder.Default
    private PenaltyStatus status = PenaltyStatus.ACTIVE;

    @CreationTimestamp
    @Column(name = "created_at", updatable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;

    @Column(name = "fine_amount")
    @Builder.Default
    private BigDecimal fineAmount = BigDecimal.ZERO;

    @OneToOne(mappedBy = "penalty", cascade = CascadeType.ALL, orphanRemoval = true)
    private Appeal appeal;
}